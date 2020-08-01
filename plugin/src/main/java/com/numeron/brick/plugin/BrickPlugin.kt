package com.numeron.brick.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class BrickPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val appExtension = project.extensions.getByType(AppExtension::class.java)
        appExtension.registerTransform(BrickTransition(project, appExtension))
    }

}