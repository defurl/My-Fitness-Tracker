package com.example.myapplication.data.repository // Adjust package

import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    fun getUserStream(userId: Long): Flow<User?> {
        return userDao.getUserByIdFlow(userId)
    }
}