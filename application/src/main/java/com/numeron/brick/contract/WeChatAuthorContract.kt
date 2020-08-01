package com.numeron.brick.contract

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.numeron.brick.WeChatAuthorsApi
import com.numeron.brick.annotation.Provide
import com.numeron.brick.annotation.Repository

@Provide
class WeChatAuthorViewModel(id: Long, provider: () -> String) : ViewModel() {

    val userLiveData = MutableLiveData<String>()

    init {
        userLiveData.postValue(provider())
    }

}

@Repository
class WeChatAuthorRepository(val weChatAuthorApi: WeChatAuthorsApi)
