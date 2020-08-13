package com.numeron.brick.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.numeron.brick.core.Brick
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Delete

class BrickPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //获取android app扩展
        val extension = project.extensions.findByType(AppExtension::class.java)
                ?: throw IllegalStateException("module ${project.name} can't apply plugin: brick. only android app.")
        //注册Brick插件
        extension.registerTransform(BrickTransform(project))
    }

}