package com.numeron.brick

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WeChatAuthor(

        @PrimaryKey
        val id: Long,

        val courseId: Int,

        val name: String,

        val order: Int,

        val parentChapterId: Int,

        val userControlSetTop: Boolean

)