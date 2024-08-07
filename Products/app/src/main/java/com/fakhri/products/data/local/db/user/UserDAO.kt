package com.fakhri.products.data.local.db.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fakhri.products.data.network.response.user.Users

@Dao
interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(users: UsersEntity)

    @Query("DELETE FROM tb_user WHERE id = :id")
    suspend fun deleteUser(id: Int)

    @Query("SELECT * FROM tb_user WHERE id = :id")
    fun getUser(id: Int): UsersEntity
}