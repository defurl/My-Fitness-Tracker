package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    // Ensure email is unique to prevent duplicate accounts using the same email
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0L, // Use 0L for Long default

    val name: String,
    val email: String,
    val passwordHash: String
)