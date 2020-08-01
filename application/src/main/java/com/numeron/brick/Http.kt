package com.numeron.brick

import com.numeron.brick.annotation.RetrofitInstance
import com.numeron.http.AbstractHttpUtil
import com.numeron.http.DateConverterFactory
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.converter.gson.GsonConverterFactory

@RetrofitInstance("retrofit")
object Http : AbstractHttpUtil() {

    private val logInterceptor = HttpLoggingInterceptor()

    init {
        logInterceptor.level = HttpLoggingInterceptor.Level.BODY
    }

    override val baseUrl: String = "https://wanandroid.com/"

    override val interceptors: Iterable<Interceptor> = listOf(
            logInterceptor
    )

    override val convertersFactories: Iterable<Converter.Factory> = listOf(
            DateConverterFactory.create("yyyy-MM-dd'T'HH:mm:ss"),
            GsonConverterFactory.create()
    )

}