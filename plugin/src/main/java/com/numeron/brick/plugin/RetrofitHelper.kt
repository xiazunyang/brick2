package com.numeron.brick.plugin

import retrofit2.Retrofit

@Suppress("unused")
class RetrofitHelper {

    private fun newRetrofit(retrofit: Retrofit, port: Int, url: String?): Retrofit {
        if (port > 0) {
            val httpUrl = retrofit.baseUrl().newBuilder().port(port).build()
            return retrofit.newBuilder().baseUrl(httpUrl).build()
        }
        if (!url.isNullOrEmpty()) {
            return retrofit.newBuilder().baseUrl(url).build()
        }
        return retrofit
    }

}