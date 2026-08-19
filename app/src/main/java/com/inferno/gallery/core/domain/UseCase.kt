package com.inferno.gallery.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base UseCase interface for coroutines.
 */
abstract class CoroutineUseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    /**
     * Executes the use case asynchronously.
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters).let {
                    Result.success(it)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}

/**
 * UseCase interface for synchronous/non-suspending operations.
 */
abstract class UseCase<in P, R> {
    operator fun invoke(parameters: P): Result<R> {
        return try {
            execute(parameters).let {
                Result.success(it)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract fun execute(parameters: P): R
}
