package com.numeron.brick

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
        val data: T,
        @SerializedName("errorMsg")
        val errorMessage: String?
)