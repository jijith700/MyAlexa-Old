package com.jijith.alexa.service.managersimpl

import android.content.Context
import com.jijith.alexa.service.database.AppDatabase
import com.jijith.alexa.service.database.AppDatabaseRepository
import com.jijith.alexa.service.interfaces.managers.DatabaseManager

class DatabaseMangerImpl(private var context: Context) : DatabaseManager {

    private val appDatabaseRepository: AppDatabaseRepository

    init {
        val userDao = AppDatabase.getDatabase(context).userDao()
        appDatabaseRepository = AppDatabaseRepository(userDao)
    }

    override fun setRefreshToken(refreshToken: String?) {
        appDatabaseRepository.setRefreshToken(refreshToken)
    }

    override fun getRefreshToken(): String {
      return appDatabaseRepository.getRefreshToken()
    }

    override fun clearRefreshToken() {
        appDatabaseRepository.clearRefreshToken()
    }
}