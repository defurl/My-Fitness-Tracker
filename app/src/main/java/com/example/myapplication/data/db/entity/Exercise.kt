package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises") // Table name
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,     // Unique ID

    val name: String,             // "Bench Press"
    val muscleGroup: String,      // "Chest"
    val imageResourceName: String? // "bench_press" (matches drawable name)
)