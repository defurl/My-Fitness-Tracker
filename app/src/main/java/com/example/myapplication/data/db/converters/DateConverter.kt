package com.example.myapplication.data.db.converters // Adjust package if needed

import androidx.room.TypeConverter
import java.util.Date

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        // If value is null, return null, otherwise create Date from timestamp
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        // If date is null, return null, otherwise get its time (timestamp)
        return date?.time
    }
}