package com.jijith.alexa.service.interfaces.managers

interface DatabaseManager {

    fun setRefreshToken(refreshToken: String?)

    fun getRefreshToken(): String

    fun clearRefreshToken()
}