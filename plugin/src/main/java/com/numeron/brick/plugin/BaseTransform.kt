package com.numeron.brick.plugin

import com.android.build.api.transform.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.io.File

abstract class BaseTransform(protected val project: Project) : Transform() {

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean = false

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun transform(transformInvocation: TransformInvocation) {

        val outputProvider = transformInvocation.outputProvider

        transformInvocation.inputs.forEach { transformInput ->

            transformInput.directoryInputs.forEach { directoryInput ->
                //复制到输入目录
                val destinationDirectory = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, destinationDirectory)
                //处理本地class
                processDirectory(destinationDirectory)
            }

            transformInput.jarInputs.forEach { jarInput ->
                //处理jar
                processJar(jarInput.file)
                //获取一个新的文件名
                var name = jarInput.name
                val md5Hex = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (name.endsWith(".jar")) {
                    name = name.substringBeforeLast('.')
                }
                //获取输出目录
                val destinationDirectory = outputProvider.getContentLocation(name + md5Hex,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                //复制到输入目录
                FileUtils.copyFile(jarInput.file, destinationDirectory)
            }
        }
    }

    protected fun iLog(message: String, vararg args: Any) {
        project.logger.log(LogLevel.INFO, message, *args)
    }

    protected fun File.forEachDeep(block: (File) -> Unit) {
        listFiles()?.forEach {
            it.forEachDeep(block)
            if (it.isFile) {
                block(it)
            }
        }
    }

    protected open fun processJar(classPath: File) {

    }

    protected open fun processDirectory(classPath: File) {

    }

}