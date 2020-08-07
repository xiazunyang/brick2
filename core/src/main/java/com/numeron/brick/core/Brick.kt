package com.numeron.brick.core

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object Brick {

    private const val OUTPUT_PROPERTY = "java.io.tmpdir"

    private const val INJECT_OUTPUT_FILE = "brick_inject.json"

    private const val ROOM_INSTANCE_OUTPUT_FILE = "brick_room_instance.json"

    private const val RETROFIT_INSTANCE_OUTPUT_FILE = "brick_retrofit_instance.json"

    private val TEMP_PATH = System.getProperty(OUTPUT_PROPERTY)

    private val injectOutputFile = File(TEMP_PATH, INJECT_OUTPUT_FILE)

    private val roomOutputFile = File(TEMP_PATH, ROOM_INSTANCE_OUTPUT_FILE)

    private val retrofitOutputFile = File(TEMP_PATH, RETROFIT_INSTANCE_OUTPUT_FILE)

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun writeInject(injectList: List<Inject>) {
        injectOutputFile.writeText(gson.toJson(injectList))
    }

    fun getInjectList(): List<Inject> {
        val readText = try {
            injectOutputFile.readText()
        } catch (e: Exception) {
            return emptyList()
        }
        return gson.fromJson(readText, object : TypeToken<List<Inject>>() {}.type)
    }

    fun setRoomInstance(roomInstanceList: List<RoomInstance>) {
        roomOutputFile.writeText(gson.toJson(roomInstanceList))
    }

    fun getRoomInstance(): List<RoomInstance> {
        val readText = try {
            roomOutputFile.readText()
        } catch (e: Exception) {
            return emptyList()
        }
        return gson.fromJson(readText, object : TypeToken<List<RoomInstance>>() {}.type)
    }

    fun setRetrofitInstance(retrofitInstance: RetrofitInstance) {
        retrofitOutputFile.writeText(gson.toJson(retrofitInstance))
    }

    fun getRetrofitInstance(): RetrofitInstance {
        val readText = try {
            retrofitOutputFile.readText()
        } catch (e: Exception) {
            throw IllegalStateException("Annotation @RetrofitInstance not found.", e)
        }
        return gson.fromJson(readText, RetrofitInstance::class.java)
    }

}