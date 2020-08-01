package com.numeron.brick

import com.numeron.brick.annotation.Port
import retrofit2.http.GET

@Port(80)
interface WeChatAuthorsApi {

    @GET("wxarticle/chapters/json")
    suspend fun getWeChatAuthorList()

}