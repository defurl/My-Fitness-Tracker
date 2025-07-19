package com.example.myapplication.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // *** Import TypeConverters ***
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.db.dao.WorkoutDao // *** Import WorkoutDao ***
import com.example.myapplication.data.db.converters.DateConverter // *** Import DateConverter ***
import com.example.myapplication.data.db.entity.* // Import all entities
import java.util.concurrent.Executors

@Database(
    entities = [
        Exercise::class,
        User::class,
        WorkoutSession::class, // *** Add WorkoutSession ***
        WorkoutExercise::class, // *** Add WorkoutExercise ***
        ExerciseSet::class      // *** Add ExerciseSet ***
    ],
    version = 6, // *** Increment version number because schema changed ***
    exportSchema = false
)
@TypeConverters(DateConverter::class) // *** Add the TypeConverter ***
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao // *** Add abstract fun for WorkoutDao ***

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("AppDatabase_DEBUG", "getDatabase called.") // ADD/VERIFY
            return INSTANCE ?: synchronized(this) {
                Log.d("AppDatabase_DEBUG", "INSTANCE is null, building database...") // ADD/VERIFY
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_fitness_app_db"
                )
                    .addCallback(roomDatabaseCallback)
                    // *** Add migration strategy (or allow destructive) ***
                    // Use this ONLY during development - it deletes data on version change!
                    .fallbackToDestructiveMigration()
                    .build()
                Log.d("AppDatabase_DEBUG", "Database build successful.")
                INSTANCE = instance
                instance
            }
        }

        // Callback remains the same (only populates exercises)
        private val roomDatabaseCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    Executors.newSingleThreadScheduledExecutor().execute {
                        populateInitialExercises(database.exerciseDao())
                    }
                }
            }
        }

        fun populateInitialExercises(exerciseDao: ExerciseDao) {
            Log.d("AppDatabase", "Attempting to populate initial exercises...") // Add log
            val initialExercises = listOf(
                // *** DEFINE EXERCISES ***
                Exercise(name = "Triceps Pushdown", muscleGroup = "Triceps", imageResourceName = "triceps_pushdown"),
                Exercise(name = "Lateral Raise", muscleGroup = "Shoulders", imageResourceName = "lateral_raise"),
                Exercise(name = "Hammer Curl", muscleGroup = "Biceps", imageResourceName = "hammer_curl"),
                Exercise(name = "Seated Shoulder Press", muscleGroup = "Shoulders", imageResourceName = "seated_shoulder_press"),
                Exercise(name = "Bicep Curl", muscleGroup = "Biceps", imageResourceName = "bicep_curl"),
                Exercise(name = "Squat", muscleGroup = "Legs", imageResourceName = "squat"),
                Exercise(name = "Deadlift", muscleGroup = "Back", imageResourceName = "deadlift"), // Add 'deadlift.png/jpg' or leave null
                Exercise(name = "Bench Press", muscleGroup = "Chest", imageResourceName = "bench_press"),
                Exercise(name = "Overhead Press", muscleGroup = "Shoulders", imageResourceName = "overhead_press"),
                Exercise(name = "Pull Up", muscleGroup = "Back", imageResourceName = "pull_up"),
                Exercise(name = "Push Up", muscleGroup = "Chest", imageResourceName = "push_up"),
                Exercise(name = "Leg Press", muscleGroup = "Legs", imageResourceName = "leg_press"),
                Exercise(name = "Leg Curl", muscleGroup = "Legs", imageResourceName = "leg_curl"),
                Exercise(name = "Calf Raise", muscleGroup = "Legs", imageResourceName = "calf_raise"),
                Exercise(name = "Plank", muscleGroup = "Abs", imageResourceName = "plank"),
                Exercise(name = "Crunches", muscleGroup = "Abs", imageResourceName = "crunches")
            )
            try {
                exerciseDao.insertAllBlocking(initialExercises)
                Log.d("AppDatabaseCallback_DEBUG", "Successfully inserted ${initialExercises.size} exercises.") // ADD/VERIFY
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error inserting initial exercises", e) // Add error log
            }
        }
    }
}