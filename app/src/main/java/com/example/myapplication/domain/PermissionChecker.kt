package com.example.myapplication.domain

/**
 * Проверка разрешений. Изолирует ViewModel от Android SDK (Environment, Build).
 */
interface PermissionChecker {
    fun isExternalStorageManager(): Boolean
}
