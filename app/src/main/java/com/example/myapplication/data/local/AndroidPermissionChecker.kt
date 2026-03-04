package com.example.myapplication.data.local

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.myapplication.domain.PermissionChecker

class AndroidPermissionChecker(private val context: Context) : PermissionChecker {
    override fun isExternalStorageManager(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
