package com.numeron.brick.plugin

import com.numeron.brick.annotation.Port
import com.numeron.brick.annotation.Url
import com.numeron.brick.core.Brick
import com.numeron.brick.core.Inject
import com.numeron.brick.core.InjectKind
import javassist.*
import javassist.bytecode.AccessFlag
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.io.File

class BrickTransition(private val project: Project) : BaseTransform() {

    private val classPool = ClassPool.getDefault()

    //获取待注入的元素列表
    private val injectList = Brick.getInjectList().groupBy(Inject::owner).toMutableMap()

    private val retrofitInstance by lazy(Brick::getRetrofitInstance)

    private val roomInstances by lazy(Brick::getRoomInstance)

    private val newRetrofitMethod by lazy {
        classPool.getCtClass("com.numeron.brick.plugin.RetrofitHelper")
                .getDeclaredMethod("newRetrofit")
    }

    init {
        classPool.appendClassPath(LoaderClassPath(javaClass.classLoader))
    }

    override fun getName(): String = "Brick"

    override fun processDirectory(classPath: File) {
        classPool.appendClassPath(classPath.absolutePath)

        injectList.mapNotNull { (owner, inject) ->
            classPool.getOrNull(owner)?.let { ctClass ->
                ctClass to inject
            }
        }.forEach { (ctClass, injectList)->
            var injectClass = ctClass
            injectList.forEach { inject ->
                injectClass = dispatchInject(injectClass, inject)
            }
            injectClass.writeFile(classPath.absolutePath)
            this.injectList.remove(ctClass.name)
        }
    }

    private fun dispatchInject(ctClass: CtClass, inject: Inject): CtClass {
        if (ctClass.isFrozen) {
            ctClass.defrost()
        }
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
        if (!url.isNullOrEmpty() || port > 0) {
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
        val annotationPort = apiClass?.getAnnotation(Port::class.java) as? Port
        return annotationUrl?.value to (annotationPort?.value ?: 0)
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

}