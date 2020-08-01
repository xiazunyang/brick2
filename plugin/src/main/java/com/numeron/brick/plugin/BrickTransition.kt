package com.numeron.brick.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class BrickTransition(private val project: Project, private val appExtension: AppExtension) : BaseTransform() {

    override fun getName(): String = "Brick"

    override fun processDirectory(directoryInput: DirectoryInput, transformInvocation: TransformInvocation) {

    }

    override fun processJar(jarInput: JarInput, transformInvocation: TransformInvocation) {

    }

    override fun onTransformed() {

    }

}