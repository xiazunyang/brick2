package com.numeron.brick

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.numeron.brick.annotation.RetrofitInstance
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private class BrickContext(base: Context) : ContextWrapper(base)

private lateinit var CONTEXT: BrickContext

fun init(application: Application) {
    CONTEXT = BrickContext(application.baseContext)
}

val okHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val logInterceptor = HttpLoggingInterceptor()
    logInterceptor.level = HttpLoggingInterceptor.Level.BODY
    OkHttpClient.Builder()
            .addInterceptor(logInterceptor)
            .callTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
}

@RetrofitInstance
val retrofit: Retrofit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://wanandroid.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}

val gson: Gson by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
}
