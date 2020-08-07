package com.numeron.brick

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WeChatAuthor::class], version = 1, exportSchema = false)
abstract class BrickDatabase : RoomDatabase() {

    abstract val weChatAuthorDao: WeChatAuthorDao

}