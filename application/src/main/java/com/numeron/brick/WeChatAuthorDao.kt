package com.numeron.brick

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeChatAuthorDao {

    @Insert
    fun insert(weChatAuthor: WeChatAuthor): Long

    @Insert
    fun insert(list: List<WeChatAuthor>): LongArray

    @Query("SELECT * FROM WeChatAuthor WHERE id = :id LIMIT 1")
    fun findById(id: Long): WeChatAuthor

    @Query("SELECT * FROM WeChatAuthor")
    fun findAll(): List<WeChatAuthor>

    @Query("DELETE FROM WeChatAuthor WHERE id = :id")
    fun delete(id: Long)

    @Delete
    fun delete(weChatAuthor: WeChatAuthor)

}