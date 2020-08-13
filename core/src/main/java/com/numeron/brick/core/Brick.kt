package com.numeron.brick.core

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object Brick {

    private const val OUTPUT_PROPERTY = "java.io.tmpdir"

    private const val INJECT_OUTPUT_FILE_NAME = "inject.json"

    private const val ROOM_INSTANCE_OUTPUT_FILE_NAME = "room.json"

    private const val RETROFIT_INSTANCE_OUTPUT_FILE = "retrofit.json"

    private val TEMP_PATH = File(System.getProperty(OUTPUT_PROPERTY), "brick")

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** 在初始化时，删除缓存里面的brick相关数据，并重新创建缓存目录 */
    fun init(projectName: String, moduleName: String) {
        val file = File(TEMP_PATH, projectName + File.separatorChar + moduleName)
        file.listFiles()?.map(File::delete)
        file.mkdirs()
    }

    fun addInject(projectName: String, moduleName: String, injectList: List<Inject>) {
        if (injectList.isEmpty()) return
        val injectPath = File(TEMP_PATH, projectName + File.separatorChar + moduleName)
        if (!injectPath.exists()) {
            injectPath.mkdirs()
        }
        File(injectPath, INJECT_OUTPUT_FILE_NAME).writeText(gson.toJson(injectList))
    }

    fun getInjectList(projectName: String): List<Inject> {
        return File(TEMP_PATH, projectName).listFiles { file ->
            file.isDirectory
        }!!.mapNotNull { directory ->
            try {
                val injectListJson = File(directory, INJECT_OUTPUT_FILE_NAME).readText()
                gson.fromJson<List<Inject>>(injectListJson, object : TypeToken<List<Inject>>() {}.type)
            } catch (throwable: Throwable) {
                null
            }
        }.flatten()
    }

    fun addRoomInstance(projectName: String, moduleName: String, roomInstanceList: List<RoomInstance>) {
        if (roomInstanceList.isEmpty()) return
        val roomPath = File(TEMP_PATH, projectName + File.separatorChar + moduleName)
        if (!roomPath.exists()) {
            roomPath.mkdirs()
        }
        File(roomPath, ROOM_INSTANCE_OUTPUT_FILE_NAME).writeText(gson.toJson(roomInstanceList))
    }

    fun getRoomInstance(projectName: String): List<RoomInstance> {
        return File(TEMP_PATH, projectName).listFiles { file ->
            file.isDirectory
        }!!.mapNotNull { directory ->
            try {
                val roomInstanceFile = File(directory, ROOM_INSTANCE_OUTPUT_FILE_NAME)
                gson.fromJson<List<RoomInstance>>(roomInstanceFile.readText(), object : TypeToken<List<RoomInstance>>() {}.type)
            } catch (e: Throwable) {
                emptyList<RoomInstance>()
            }
        }.flatten()
    }

    fun setRetrofitInstance(projectName: String, retrofitInstance: RetrofitInstance) {
        val retrofitPath = File(TEMP_PATH, projectName)
        if (!retrofitPath.exists()) {
            retrofitPath.mkdirs()
        }
        val retrofitFile = File(retrofitPath, RETROFIT_INSTANCE_OUTPUT_FILE)
        retrofitFile.writeText(gson.toJson(retrofitInstance))
    }

    fun getRetrofitInstance(projectName: String): RetrofitInstance {
        return try {
            val retrofitFile = File(TEMP_PATH, projectName + File.separatorChar + RETROFIT_INSTANCE_OUTPUT_FILE)
            gson.fromJson(retrofitFile.readText(), RetrofitInstance::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Annotation @RetrofitInstance not found.")
        }
    }

    fun clearProjectCache(projectName: String) {
        File(TEMP_PATH, projectName).delete()
    }

    /**
     * 当缓存目录下存在此模块的数据时，返回true
     * @param projectName String
     * @param moduleName String
     * @return Boolean
     */
    fun isInjectModule(projectName: String, moduleName: String): Boolean {
        return File(TEMP_PATH, projectName + File.separatorChar + moduleName).exists()
    }

}