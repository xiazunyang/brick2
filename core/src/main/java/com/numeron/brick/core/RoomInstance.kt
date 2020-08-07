package com.numeron.brick.core

import java.lang.IllegalStateException

data class RoomInstance(

        val name: String,

        val owner: String,

        val kind: InjectableKind,

        val dao: List<DaoMethod>) {

    fun getInvoke(type: String): String? {
        val daoMethod = dao.find { it.type == type } ?: return null
        return when (kind) {
            InjectableKind.Object -> "$owner.INSTANCE.$name()"
            InjectableKind.Class, InjectableKind.Companion -> "$owner.$name()"
            else -> throw IllegalStateException("Not supported injected method.")
        }.let {
            "$it.${daoMethod.name}()"
        }
    }

}