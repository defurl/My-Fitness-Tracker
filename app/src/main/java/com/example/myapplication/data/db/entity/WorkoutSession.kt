package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["sessionUserId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionUserId"])]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    val sessionUserId: Long, // FK to User
    val startTime: Date,
    val endTime: Date?,      // Set when finished
    val durationMillis: Long?, // Calculated duration
    val totalSets: Int = 0,
    var workoutName: String? = null
)