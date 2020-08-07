package com.numeron.brick

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.numeron.brick.annotation.RoomInstance

class BrickApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        init(this)
    }

    companion object {

        private var brickDatabase: BrickDatabase? = null
        private lateinit var context: Context

        @RoomInstance
        fun getBrickDatabase(): BrickDatabase {
            if (brickDatabase == null) {
                brickDatabase = Room.databaseBuilder(context, BrickDatabase::class.java, "brick.db").build()
            }
            return brickDatabase!!
        }

    }

}