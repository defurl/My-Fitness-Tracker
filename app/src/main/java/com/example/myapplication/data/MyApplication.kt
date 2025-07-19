package com.example.myapplication.data

import android.app.Application
import android.util.Log // Import Log
import com.example.myapplication.data.db.AppDatabase

class MyApplication : Application() {
    init {
        Log.d("MyApplication_DEBUG", "MyApplication instance CREATED.") // ADD/VERIFY
    }
    val database: AppDatabase by lazy {
        Log.d("MyApplication_DEBUG", "Database lazy property ACCESSED.") // ADD/VERIFY
        AppDatabase.getDatabase(this)
    }
    override fun onCreate() { // Also log the standard Application onCreate
        super.onCreate()
        Log.d("MyApplication_DEBUG", "MyApplication onCreate() called.") // ADD/VERIFY
    }
}