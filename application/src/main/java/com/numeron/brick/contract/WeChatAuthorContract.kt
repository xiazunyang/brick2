package com.numeron.brick.contract

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numeron.brick.ApiResponse
import com.numeron.brick.WeChatAuthor
import com.numeron.brick.WeChatAuthorDao
import com.numeron.brick.annotation.Inject
import com.numeron.brick.annotation.Port
import com.numeron.brick.annotation.Provide
import kotlinx.coroutines.launch
import retrofit2.http.GET

@Provide
class WeChatAuthorViewModel : ViewModel() {

    @Inject
    private lateinit var weChatAuthorRepository: WeChatAuthorRepository

    val weChatAuthorLiveData = MutableLiveData<List<WeChatAuthor>>()

    fun getWeChatAuthorList() {
        viewModelScope.launch {
            val weChatAuthorList = weChatAuthorRepository.weChatAuthorsApi.getWeChatAuthorList()
            weChatAuthorLiveData.postValue(weChatAuthorList.data)
        }
    }

}

class WeChatAuthorRepository {

    @Inject
    lateinit var weChatAuthorsApi: WeChatAuthorsApi

    @Inject
    lateinit var weChatAuthorDao: WeChatAuthorDao

}

interface WeChatAuthorsApi {

    @GET("wxarticle/chapters/json")
    suspend fun getWeChatAuthorList(): ApiResponse<List<WeChatAuthor>>

}