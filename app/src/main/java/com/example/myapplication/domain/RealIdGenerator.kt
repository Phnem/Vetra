package com.example.myapplication.domain

import java.util.UUID

class RealIdGenerator : IdGenerator {
    override fun generateUuid(): String = UUID.randomUUID().toString()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
