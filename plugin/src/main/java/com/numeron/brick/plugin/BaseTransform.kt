package com.numeron.brick.plugin

import com.android.build.api.transform.*
import org.apache.commons.io.FileUtils
import java.io.File

abstract class BaseTransform : Transform() {

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean = true

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun transform(transformInvocation: TransformInvocation) {

        val outputProvider = transformInvocation.outputProvider

        transformInvocation.inputs.forEach { transformInput ->

            transformInput.jarInputs.forEach { jarInput ->
                //复制到输入目录
                val destinationDirectory = outputProvider.getContentLocation(jarInput.file.absolutePath,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, destinationDirectory)
                //处理jar
                processJar(destinationDirectory)
            }

            //处理本地class
            transformInput.directoryInputs.forEach { directoryInput ->
                //复制到输入目录
                val destinationDirectory = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, destinationDirectory)
                //处理文件
                processDirectory(destinationDirectory)
            }

        }

        onTransformed()
    }

    protected open fun onTransformed() {

    }

    protected open fun processJar(classPath: File) {

    }

    protected open fun processDirectory(classPath: File) {

    }

}