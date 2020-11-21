package com.jijith.alexa.service.database

import com.jijith.alexa.utils.EMPTY_STRING
import com.jijith.alexa.vo.User
import kotlinx.coroutines.*
import timber.log.Timber

class AppDatabaseRepository(private val userDao: UserDao) {

    fun setRefreshToken(refreshToken: String?) {
        GlobalScope.launch (Dispatchers.Main) {
            val user = User(1, "", "", refreshToken);
            userDao.insert(user)
            Timber.d("user %s", user);
        }
    }

    fun getRefreshToken() :Deferred<String> {
        var refreshToken = EMPTY_STRING
        GlobalScope.launch (Dispatchers.Main) {
            refreshToken = userDao.getRefreshToken(1)
            Timber.d("refresh token: %s", refreshToken);
        }
        return GlobalScope.async(Dispatchers.IO) {
            refreshToken = userDao.getRefreshToken(1)
            refreshToken
        }
    }

    fun clearRefreshToken() {
        GlobalScope.launch (Dispatchers.Main) {
//            userDao.delete(userDao.get(1))
        }
    }
}