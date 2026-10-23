package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChineseWhispersTest {

    @Test
    fun cluster_twoDistinctCliques_partitionsCorrectly() {
        val nodes = listOf(1L, 2L, 3L, 10L, 11L, 12L)

        // Cluster 1: nodes 1, 2, 3 strongly connected (weight 0.9)
        // Cluster 2: nodes 10, 11, 12 strongly connected (weight 0.9)
        // Weak noise edge between node 3 and 10 (weight 0.1)
        val edges = mutableMapOf<Long, MutableList<ChineseWhispers.Edge>>()

        fun addUndirectedEdge(u: Long, v: Long, weight: Float) {
            edges.getOrPut(u) { mutableListOf() }.add(ChineseWhispers.Edge(v, weight))
            edges.getOrPut(v) { mutableListOf() }.add(ChineseWhispers.Edge(u, weight))
        }

        // Clique 1
        addUndirectedEdge(1L, 2L, 0.9f)
        addUndirectedEdge(2L, 3L, 0.9f)
        addUndirectedEdge(1L, 3L, 0.9f)

        // Clique 2
        addUndirectedEdge(10L, 11L, 0.9f)
        addUndirectedEdge(11L, 12L, 0.9f)
        addUndirectedEdge(10L, 12L, 0.9f)

        // Weak noise bridge
        addUndirectedEdge(3L, 10L, 0.1f)

        val result = ChineseWhispers.cluster(nodes, edges)

        // Nodes in clique 1 should have identical cluster IDs
        val cluster1 = result[1L]
        assertEquals(cluster1, result[2L])
        assertEquals(cluster1, result[3L])

        // Nodes in clique 2 should have identical cluster IDs
        val cluster2 = result[10L]
        assertEquals(cluster2, result[11L])
        assertEquals(cluster2, result[12L])

        // The two cliques must be distinct
        assertNotEquals(cluster1, cluster2)

        val grouped = ChineseWhispers.groupClusters(result)
        assertEquals(2, grouped.size)
        assertTrue(grouped.values.any { it.toSet() == setOf(1L, 2L, 3L) })
        assertTrue(grouped.values.any { it.toSet() == setOf(10L, 11L, 12L) })
    }

    @Test
    fun cluster_disconnectedNodes_remainSingletons() {
        val nodes = listOf(1L, 2L, 3L)
        val edges = emptyMap<Long, List<ChineseWhispers.Edge>>()

        val result = ChineseWhispers.cluster(nodes, edges)
        assertEquals(1L, result[1L])
        assertEquals(2L, result[2L])
        assertEquals(3L, result[3L])
    }

    @Test
    fun cluster_emptyGraph_returnsEmpty() {
        val result = ChineseWhispers.cluster(emptyList(), emptyMap())
        assertTrue(result.isEmpty())
    }
}
