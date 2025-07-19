package com.example.myapplication.data.db.dao // Adjust package if needed

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.db.entity.User
import kotlinx.coroutines.flow.Flow // Import Flow

@Dao
interface UserDao {

    // Inserts a new user. If a user with the same email tries to register,
    // the insertion will fail due to the unique index on email.
    // We might want to handle this conflict more explicitly later.
    // Using OnConflictStrategy.ABORT (default) or FAIL is appropriate.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long // Returns the new userId, or throws exception on conflict

    // Finds a user by their email address. Used during login.
    // Returns null if no user is found with that email.
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    // Finds a user by their unique ID. May be useful later.
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    // Finds a user by their unique ID. Returns a Flow for observing changes.
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<User?>



}