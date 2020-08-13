package com.numeron.brick.plugin

import com.numeron.brick.annotation.Port
import com.numeron.brick.annotation.Url
import com.numeron.brick.core.*
import javassist.*
import javassist.bytecode.AccessFlag
import org.gradle.api.Project
import java.io.File
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class BrickTransform(project: Project) : BaseTransform(project) {

    private val projectName = project.rootProject.name

    private val moduleName = project.name

    private val classPool = ClassPool.getDefault()

    private val modulePathList = mutableListOf<File>()

    //获取待注入的元素列表
    private val injectMap by lazy {
        Brick.getInjectList(projectName).groupBy(Inject::owner).toMutableMap()
    }

    private val retrofitInstance by lazy {
        Brick.getRetrofitInstance(projectName)
    }

    private val roomInstances by lazy {
        Brick.getRoomInstance(projectName)
    }

    private val newRetrofitMethod by lazy {
        classPool.getCtClass("com.numeron.brick.plugin.RetrofitTemplate")
                .getDeclaredMethod("newRetrofit")
    }

    init {
        classPool.appendClassPath(LoaderClassPath(javaClass.classLoader))
    }

    override fun getName(): String = "Brick"

    override fun processJar(jarName: String, jarFile: File) {
        if (jarName.startsWith(':') && Brick.isInjectModule(projectName, jarName.substring(1))) {
            //获取并创建jar解压路径
            val unzipPath = File(jarFile.parent, jarFile.name.substringBeforeLast('.'))
            if (!unzipPath.exists()) unzipPath.mkdirs()
            //解压jar
            val zipFile = ZipFile(jarFile)
            unzip(zipFile, unzipPath)
            //添加到池中
            classPool.appendClassPath(unzipPath.absolutePath)
            //添加到待处理模块列表中
            modulePathList.add(unzipPath)
            /* 不可以在这里处理，因为处理的顺序与依赖的顺序不一样，先加到列表中，最后再在onTransformed方法中统一处理 */
        }
    }

    override fun processDirectory(classPath: File) {
        scanInjectModule(classPath)
    }

    override fun onTransformed() {
        modulePathList.forEach { classPath ->
            //处理模块的class
            scanInjectModule(classPath)
            //重新打包成jar文件并替换原文件
            val jarFile = File(classPath.parentFile, classPath.name + ".jar")
            zip(classPath, jarFile)
        }
        //处理完成后，清理缓存文件
        //Brick.clearProjectCache(projectName)
    }

    private fun scanInjectModule(classPath: File) {
        classPool.appendClassPath(classPath.absolutePath)

        classPath.forEachDeep {
            //获取此class的相对路径
            val classFilePath = it.absolutePath
                    .substringBeforeLast('.')
                    .removePrefix(classPath.absolutePath + File.separatorChar)
                    .replace(File.separatorChar, '.')
            prepareInject(classPath.absolutePath, classFilePath)
        }
    }

    private fun prepareInject(classPath: String, classFilePath: String) {
        //从缓存列表中找到对应的注入项
        val injectList = injectMap.remove(classFilePath)
        if (injectList != null) {
            //获取CtClass
            var injectClass = classPool.get(classFilePath)
            //解冻
            val frozen = injectClass.isFrozen
            //frozen为false时，则开始注入，否则直接写到文件中
            if (!frozen) {
                //注入
                injectList.forEach { inject ->
                    injectClass = dispatchInject(injectClass, inject)
                }
            }
            iLog("injected: ${injectClass.name}")
            //写入到文件中
            injectClass.writeFile(classPath)
        }
    }

    private fun dispatchInject(ctClass: CtClass, inject: Inject): CtClass {
        return when (inject.kind) {
            InjectKind.Room -> injectDao(ctClass, inject)
            InjectKind.Retrofit -> injectApi(ctClass, inject)
            InjectKind.Class -> injectInstance(ctClass, inject)
        }
    }

    private fun injectApi(ctClass: CtClass, inject: Inject): CtClass {
        val injectField = ctClass.getDeclaredField(inject.variableName)

        //移除已存在的、未初始化的字段
        ctClass.removeField(injectField)

        //添加final修饰符
        injectField.modifiers += Modifier.FINAL
        injectField.modifiers = AccessFlag.setPrivate(injectField.modifiers)

        //添加初始化代码，并重新添加到class中
        val retrofitGetter = retrofitInstance.getInstance()
        //导包
        import(retrofitInstance.owner)

        val variableType = inject.variableType
        val (url, port) = getUrlAndPort(inject)
        if ((!url.isNullOrEmpty() || port > 0) && !hasNewRetrofitMethod(ctClass)) {
            ctClass.addMethod(CtMethod(newRetrofitMethod, ctClass, null))
            ctClass.addField(injectField, "newRetrofit($retrofitGetter, $port, $url).create(${variableType}.class);")
        } else {
            ctClass.addField(injectField, "${retrofitGetter}.create(${variableType}.class);")
        }

        //移除掉相关的setter方法
        removeSetter(ctClass, injectField.name)

        return ctClass
    }

    private fun getUrlAndPort(inject: Inject): Pair<String?, Int> {
        val apiClass = classPool.getOrNull(inject.variableType)
        val annotationUrl = apiClass?.getAnnotation(Url::class.java) as? Url
        val url = annotationUrl?.value?.let {
            "\"$it\""
        }
        val annotationPort = apiClass?.getAnnotation(Port::class.java) as? Port
        val port = annotationPort?.value ?: 0
        return url to port
    }

    private fun removeSetter(ctClass: CtClass, fieldName: String) {
        try {
            val methodName = "set" + fieldName[0].toUpperCase() + fieldName.substring(1)
            val ctMethod = ctClass.getDeclaredMethod(methodName)
            ctClass.removeMethod(ctMethod)
        } catch (ignore: Throwable) {
        }
    }

    private fun injectDao(ctClass: CtClass, inject: Inject): CtClass {
        val injectField = ctClass.getDeclaredField(inject.variableName)
        //移除已存在的、未初始化的字段
        ctClass.removeField(injectField)

        //添加final修饰符
        injectField.modifiers += Modifier.FINAL
        injectField.modifiers = AccessFlag.setPrivate(injectField.modifiers)

        //找到可以注入的ROOM对象和可用的DAO方法
        val roomInstance = roomInstances.find {
            it.getInvoke(inject.variableType) != null
        } ?: throw IllegalStateException("Type:${inject.variableType} not found.")

        //导包
        import(roomInstance.owner)
        //获取创建的代码
        val invoke = roomInstance.getInvoke(inject.variableType)!!
        //重新添加字段
        ctClass.addField(injectField, "$invoke;")

        //移除掉setter方法
        removeSetter(ctClass, injectField.name)

        return ctClass
    }

    private fun injectInstance(ctClass: CtClass, inject: Inject): CtClass {
        val injectField = ctClass.getDeclaredField(inject.variableName)
        //移除旧的字段
        ctClass.removeField(injectField)
        //修改修饰符
        injectField.modifiers += Modifier.FINAL
        injectField.modifiers = AccessFlag.setPrivate(injectField.modifiers)
        //重新添加字段，并通过默认构造创建默认值。
        ctClass.addField(injectField, "new ${injectField.type.name}();")
        return ctClass
    }

    /** 检查是否已经导包，如果没有，则导入这个包下的类，如果已导入，则忽略 */
    private fun import(classPath: String) {
        if (!classPool.importedPackages.asSequence().contains(classPath)) {
            classPool.importPackage(classPath)
        }
    }

    private fun hasNewRetrofitMethod(ctClass: CtClass): Boolean {
        return try {
            ctClass.getDeclaredMethod("newRetrofit", newRetrofitMethod.parameterTypes)
            true
        } catch (throwable: Throwable) {
            false
        }
    }

    /**
     * 解压jar文件
     * @param zipFile ZipFile 要解压的文件
     * @param outputPath File 要输入的目录，必需是文件夹
     */
    private fun unzip(zipFile: ZipFile, outputPath: File) {
        for (zipEntry in zipFile.entries()) {
            if (zipEntry.isDirectory) {
                continue
            }
            //获取并创建存放目录
            val entryFile = File(outputPath, zipEntry.name.replace('/', File.separatorChar))
            if (!entryFile.parentFile.exists()) entryFile.parentFile.mkdirs()
            //通过流复制到存放文件中
            zipFile.getInputStream(zipEntry).copyTo(entryFile.outputStream())
        }
    }

    /**
     * 压缩一个文件夹为jar文件
     * @param inputPath File 要压缩的文件夹
     * @param outputFile File 压缩后输入的文件，是一个文件
     */
    private fun zip(inputPath: File, outputFile: File) {
        JarOutputStream(outputFile.outputStream()).use { jarOutputStream ->
            inputPath.forEachDeep { file ->
                val zipEntry = ZipEntry(file.path.removePrefix(inputPath.path + File.separatorChar))
                jarOutputStream.putNextEntry(zipEntry)
                file.inputStream().copyTo(jarOutputStream)
            }
        }
    }

}