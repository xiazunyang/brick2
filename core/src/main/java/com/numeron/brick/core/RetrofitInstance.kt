package com.numeron.brick.core

data class RetrofitInstance(

        val name: String,

        val owner: String,

        val kind: InjectableKind

) {

    fun getInstance(): String {
        return when (kind) {
            InjectableKind.Object -> "$owner.INSTANCE"
            InjectableKind.Class, InjectableKind.Companion -> owner
            else -> throw IllegalStateException("Not supported injected method.")
        }.let {
            "$it.$name()"
        }
    }

}