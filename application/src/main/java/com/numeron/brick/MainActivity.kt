package com.numeron.brick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private val userId: String = "Hello world."

//    private val weChatAuthorViewModel by lazyWeChatAuthorViewModel(0, ::userId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}