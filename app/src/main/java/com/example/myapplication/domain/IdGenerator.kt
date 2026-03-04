package com.example.myapplication.domain

/**
 * Генератор идентификаторов и времени. Позволяет тестировать UseCase с детерминированными значениями.
 */
interface IdGenerator {
    fun generateUuid(): String
    fun currentTimeMillis(): Long
}
