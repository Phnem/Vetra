package com.example.myapplication.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApiRateLimiter {
    private val mutex = Mutex()
    suspend fun <T> executeSafe(block: suspend () -> T): T {
        mutex.withLock {
            delay(1200)
            return block()
        }
    }
}
