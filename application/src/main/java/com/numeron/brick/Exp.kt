package com.numeron.brick

import com.numeron.brick.annotation.RetrofitInstance

@RetrofitInstance
val retrofit
    get() = Http.retrofit