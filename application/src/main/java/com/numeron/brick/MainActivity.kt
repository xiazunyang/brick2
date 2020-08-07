package com.numeron.brick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import com.numeron.brick.contract.lazyWeChatAuthorViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val weChatAuthorViewModel by lazyWeChatAuthorViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView.setOnClickListener {
            weChatAuthorViewModel.getWeChatAuthorList()
        }
        weChatAuthorViewModel.weChatAuthorLiveData.observe(this, Observer {
            Toast.makeText(this, "Got weChatAuthors", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", it.toString())
        })
    }

}