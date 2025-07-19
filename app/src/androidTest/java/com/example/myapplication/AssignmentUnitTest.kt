package com.example.myapplication // Adjust package to match your androidTest source set root

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.db.dao.WorkoutDao
import com.example.myapplication.data.db.entity.Exercise
import com.example.myapplication.data.db.entity.ExerciseSet
import com.example.myapplication.data.db.entity.User
import com.example.myapplication.data.db.entity.WorkoutExercise
import com.example.myapplication.data.db.entity.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class AssignmentUnitTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        exerciseDao = database.exerciseDao()
        workoutDao = database.workoutDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // --- Simple DAO Tests ---

    @Test
    @Throws(Exception::class)
    fun insertAndGetUser() = runBlocking {
        val user = User(userId = 1L, name = "Test User", email = "test@test.com", passwordHash = "hashed_pw")
        userDao.insertUser(user)
        val retrievedUser = userDao.getUserByEmail("test@test.com")
        assertNotNull("Retrieved user should not be null", retrievedUser)
        assertEquals("User name should match", "Test User", retrievedUser?.name)
        assertEquals("User email should match", "test@test.com", retrievedUser?.email)
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetWorkoutSession() = runBlocking {
        val user = User(userId = 55L, name = "History User", email = "history@test.com", passwordHash = "hash")
        userDao.insertUser(user)

        // Fix #4 & #5: Add null for nullable parameters
        val session = WorkoutSession(
            sessionUserId = 55L,
            startTime = Date(1678880000000L),
            endTime = null, // Provide null
            durationMillis = null, // Provide null
            totalSets = 15,
            workoutName = "Morning Routine"
        )
        val sessionId = workoutDao.insertWorkoutSession(session)
        val history = workoutDao.getWorkoutSessionsForUser(55L).first()

        assertTrue("History should not be empty", history.isNotEmpty())
        assertEquals("Should retrieve 1 session", 1, history.size)
        assertEquals("Workout name should match", "Morning Routine", history[0].workoutName)
        assertEquals("Total sets should match", 15, history[0].totalSets)
        assertEquals("Session ID should match", sessionId, history[0].sessionId)
    }

    @Test
    @Throws(Exception::class)
    fun insertWorkoutDetailsAndGetSets() = runBlocking {
        val user = User(userId = 77L, name = "Set User", email = "sets@test.com", passwordHash = "h")
        userDao.insertUser(user)
        val exercise = Exercise(exerciseId = 99L, name = "Set Ex", muscleGroup = "Test", imageResourceName = null)
        exerciseDao.insertAllBlocking(listOf(exercise))

        val session = WorkoutSession(
            sessionUserId = 77L,
            startTime = Date(),
            endTime = null, // Provide null
            durationMillis = null, // Provide null
            totalSets = 3, // Corrected count based on sets below
            workoutName = "Set Test Session"
        )
        val sessionId = workoutDao.insertWorkoutSession(session)
        val workoutExercise = WorkoutExercise(workoutSessionId = sessionId, workoutExerciseRefId = 99L)
        val workoutExerciseId = workoutDao.insertWorkoutExercise(workoutExercise)
        val set1 = ExerciseSet(setWorkoutExerciseId = workoutExerciseId, setNumber = 1, reps = 12, weight = 20.0, isCompleted = true)
        val set2 = ExerciseSet(setWorkoutExerciseId = workoutExerciseId, setNumber = 2, reps = 10, weight = 20.0, isCompleted = true)
        val set3 = ExerciseSet(setWorkoutExerciseId = workoutExerciseId, setNumber = 3, reps = 8, weight = 22.5, isCompleted = false)
        workoutDao.insertAllExerciseSets(listOf(set1, set2, set3))

        // Fix #6: Ensure method exists in WorkoutDao and name matches exactly
        val retrievedSets = workoutDao.getSetsForWorkoutSession(sessionId)

        assertEquals("Should retrieve 3 sets", 3, retrievedSets.size)
        assertEquals("Set 1 weight", 20.0, retrievedSets[0].weight, 0.001)
        assertEquals("Set 3 reps", 8, retrievedSets[2].reps)
        assertFalse("Set 3 completion", retrievedSets[2].isCompleted)
    }
}