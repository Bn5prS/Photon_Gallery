package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regression tests for GitHub Issue #58 (FORTIFY: pthread_mutex_lock called on a destroyed mutex
 * inside libonnxruntime.so OrtSession.run). Verifies that [RefCountedResourceGate] guarantees
 * a native session can never be closed while an inference call or another worker lease is active.
 */
class SmartSearchSessionConcurrencyTest {

    private class FakeNativeSession(val id: Int) {
        val closed = AtomicBoolean(false)
        val inFlightRuns = AtomicInteger(0)
        val useAfterCloseDetected = AtomicBoolean(false)
        val closeWhileRunningDetected = AtomicBoolean(false)

        fun run(): Int {
            if (closed.get()) {
                useAfterCloseDetected.set(true)
                error("OrtSession.run invoked on destroyed session #$id")
            }
            inFlightRuns.incrementAndGet()
            try {
                // Simulate native inference window
                Thread.yield()
                if (closed.get()) {
                    useAfterCloseDetected.set(true)
                    error("OrtSession #$id destroyed mid-inference")
                }
                return id
            } finally {
                inFlightRuns.decrementAndGet()
            }
        }

        fun close() {
            if (inFlightRuns.get() > 0) {
                closeWhileRunningDetected.set(true)
            }
            closed.set(true)
        }
    }

    @Test
    fun overlappingWorkerLeasesKeepSessionAliveUntilLastRelease() {
        val gate = RefCountedResourceGate<FakeNativeSession>()
        val created = AtomicInteger(0)
        val sessions = mutableListOf<FakeNativeSession>()
        val loader = {
            FakeNativeSession(created.incrementAndGet()).also { sessions += it }
        }

        val s1 = gate.acquire(loader)
        val s2 = gate.acquire(loader)
        assertEquals(s1.id, s2.id)
        assertEquals(2, gate.activeLeaseCount())
        assertTrue(gate.isLoaded())

        // Worker 1 finishes (or is cancelled by ExistingWorkPolicy.REPLACE) while Worker 2 is running
        gate.release(closer = { it.close() })
        assertFalse("Session must remain open while Worker 2 still holds a lease", s1.closed.get())
        assertTrue(gate.isLoaded())
        assertEquals(1, gate.activeLeaseCount())

        // Unload request while leased must be a safe no-op
        assertFalse(gate.unloadIfUnused(closer = { it.close() }))
        assertFalse(s1.closed.get())

        // Worker 2 finishes — now session closes cleanly
        gate.release(closer = { it.close() })
        assertTrue("Session should close once last lease is released", s1.closed.get())
        assertFalse(gate.isLoaded())
        assertEquals(0, gate.activeLeaseCount())
    }

    @Test
    fun concurrentInferenceUnloadAndForceCloseNeverUseAfterClose() {
        val gate = RefCountedResourceGate<FakeNativeSession>()
        val created = AtomicInteger(0)
        val allSessions = java.util.Collections.synchronizedList(mutableListOf<FakeNativeSession>())
        val loader = {
            FakeNativeSession(created.incrementAndGet()).also { allSessions += it }
        }

        val threads = 8
        val pool = Executors.newFixedThreadPool(threads)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threads)
        val failureCount = AtomicInteger(0)

        for (t in 0 until threads) {
            pool.submit {
                try {
                    startLatch.await()
                    for (iter in 0 until 200) {
                        when ((t + iter) % 4) {
                            0 -> {
                                gate.withResource(loader) { session -> session.run() }
                            }
                            1 -> {
                                gate.acquire(loader)
                                try {
                                    gate.withResource(loader) { session -> session.run() }
                                } finally {
                                    gate.release(closer = { it.close() })
                                }
                            }
                            2 -> {
                                gate.unloadIfUnused(closer = { it.close() })
                            }
                            3 -> {
                                if (iter % 15 == 0) {
                                    gate.forceClose(closer = { it.close() })
                                } else {
                                    gate.withResource(loader) { session -> session.run() }
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    failureCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue("Stress test timed out", doneLatch.await(15, TimeUnit.SECONDS))
        pool.shutdownNow()

        assertEquals("Unexpected exceptions during concurrent inference/close", 0, failureCount.get())
        assertTrue("Expected at least one session to be created", allSessions.isNotEmpty())
        assertTrue(
            "Detected use-after-close on an OrtSession",
            allSessions.none { it.useAfterCloseDetected.get() }
        )
        assertTrue(
            "Detected close() while OrtSession.run() was in flight",
            allSessions.none { it.closeWhileRunningDetected.get() }
        )
    }
}
