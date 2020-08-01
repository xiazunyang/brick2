package com.numeron.brick.plugin

import com.android.build.api.transform.*
import javassist.ClassPool
import org.apache.commons.io.FileUtils

abstract class BaseTransform : Transform() {

    protected val classPool = ClassPool.getDefault()

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
            //处理第三方jar
            transformInput.jarInputs.forEach { jarInput ->
                //添加到池中
                classPool.appendClassPath(jarInput.file.absolutePath)

                //处理jar
                processJar(jarInput, transformInvocation)
                //复制到输入目录
                val destination = outputProvider.getContentLocation(jarInput.file.absolutePath,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, destination)
            }
            //处理本地class
            transformInput.directoryInputs.forEach { directoryInput ->
                //添加到池中
                classPool.appendClassPath(directoryInput.file.absolutePath)
                //处理文件夹
                processDirectory(directoryInput, transformInvocation)
                //复制到输入目录
                val destination = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, destination)
            }

        }

        onTransformed()
    }

    protected open fun onTransformed() {

    }

    protected open fun processJar(jarInput: JarInput, transformInvocation: TransformInvocation) {

    }

    protected open fun processDirectory(directoryInput: DirectoryInput, transformInvocation: TransformInvocation) {

    }

}