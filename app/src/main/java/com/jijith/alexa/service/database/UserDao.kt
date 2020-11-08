package com.jijith.alexa.service.database

import android.provider.ContactsContract.CommonDataKinds.Note
import androidx.lifecycle.LiveData
import androidx.room.*
import com.jijith.alexa.vo.User


@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE uid IN (:userId)")
    suspend fun get(userId: Int): User

    @Query("SELECT refresh_token FROM user WHERE uid = :userId")
    suspend fun getRefreshToken(vararg userId: Int): String
    
    /*@Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): User*/

    @Insert
    suspend fun insert(vararg users: User)

    @Update
    suspend fun update(user: User)
    
    @Delete
    suspend fun delete(user: User)
}