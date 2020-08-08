package com.numeron.brick.processor

import com.bennyhuo.aptutils.types.asElement
import com.bennyhuo.aptutils.types.simpleName
import com.numeron.brick.annotation.RoomInstance as ARoom
import com.numeron.brick.core.*
import com.numeron.brick.annotation.Inject as AInject
import com.numeron.brick.annotation.RetrofitInstance as ARetrofit
import com.sun.tools.javac.code.Symbol
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class InjectProcessor {

    fun process(env: RoundEnvironment) {
        //处理RetrofitInstance注解
        retrofitProcess(env)

        //处理RoomInstance注解
        roomProcess(env)

        //获取所有被Inject注解标记的类，并输入到inject.json中
        Brick.writeInject(env.getElementsAnnotatedWith(AInject::class.java).map(::inject))

    }

    private fun retrofitProcess(env: RoundEnvironment) {
        val annotatedElements = env.getElementsAnnotatedWith(ARetrofit::class.java)

        if (annotatedElements.size == 0) {
            throw IllegalStateException("Annotation @RetrofitInstance not found.")
        }

        if (annotatedElements.size > 1) {
            throw IllegalStateException("Annotation @RetrofitInstance only use 1 times!")
        }

        val element = annotatedElements.firstOrNull() ?: return

        val ownerClassSymbol = element.enclosingElement as Symbol.ClassSymbol

        val methodSymbol = ownerClassSymbol.members()
                .getElements {
                    it is Symbol.MethodSymbol && it.returnType.toString() == RETROFIT_CLASS_NAME
                }
                .firstOrNull() as? Symbol.MethodSymbol
                ?: throw IllegalStateException("The method whose return type is Retrofit was not found.")

        val name = methodSymbol.simpleName()
        val owner = ownerClassSymbol.toString()
        val kind = getKind(ownerClassSymbol)

        Brick.setRetrofitInstance(RetrofitInstance(name, owner, kind))
    }

    private fun inject(element: Element): Inject {
        val variableName = element.simpleName.toString()

        val asElement = element.asType().asElement()

        val variableType = asElement.toString()

        val ownerClassName = element.enclosingElement.toString()

        if (asElement is Symbol.ClassSymbol && asElement.isInterface) {
            //如果是接口，则判断是Room的DAO还是Retrofit的API
            var isRoomDao = false
            var isRetrofitApi = false

            asElement.members()
                    //获取接口上所有的方法
                    .getElements {
                        it is ExecutableElement
                    }
                    .forEach { symbol ->
                        //根据方法上的注解的包名来判断是Room的Dao接口，还是Retrofit的Api接口
                        val annotationMirrors = symbol.annotationMirrors
                        isRetrofitApi = !isRoomDao && annotationMirrors.any {
                            it.toString().contains(RETROFIT_NAME_SPACE)
                        }
                        isRoomDao = !isRetrofitApi && annotationMirrors.any {
                            it.toString().contains(ROOM_NAME_SPACE)
                        }
                    }
            return when {
                isRoomDao -> Inject(variableName, variableType, ownerClassName, InjectKind.Room)
                isRetrofitApi -> Inject(variableName, variableType, ownerClassName, InjectKind.Retrofit)
                else -> Inject(variableName, variableType, ownerClassName, InjectKind.Class)
            }
        }
        return Inject(variableName, variableType, ownerClassName, InjectKind.Class)
    }

    private fun getKind(element: Element): InjectableKind {
        if (element is Symbol.ClassSymbol) {
            //判断这个类是否是object单例
            element.members()
                    .getElements {
                        it.name.contentEquals("INSTANCE")
                    }
                    .firstOrNull {
                        it.isStatic && it.modifiers.containsAll(listOf(Modifier.PUBLIC, Modifier.STATIC))
                    }
                    ?.let {
                        return InjectableKind.Object
                    }

            //判断这个类是否是Companion对象
            val isSubClass = element.enclosingElement is TypeElement
            if (isSubClass) {
                val outerClass = element.enclosingElement as Symbol.TypeSymbol
                val companionMember = outerClass.members().getElementsByName(element.name)
                        .firstOrNull()
                        ?: throw IllegalStateException("Annotation @RetrofitInstance only use on object class and companion object.")
                if (companionMember is Symbol.VarSymbol) {
                    val isPublicAndStatic = element.modifiers.containsAll(listOf(Modifier.PUBLIC, Modifier.STATIC))
                    if (!isPublicAndStatic) throw IllegalStateException("Annotation @RetrofitInstance only use on companion object.")
                    return InjectableKind.Companion
                }
            }
            return InjectableKind.Class
        } else if (element is Symbol.MethodSymbol) {
            val isEmptyArg = element.params.isEmpty()
            if (isEmptyArg) {
                return if (element.name.startsWith("get")) {
                    InjectableKind.Getter
                } else {
                    InjectableKind.Method
                }
            }
        } else if (element is Symbol.VarSymbol) {
            return InjectableKind.Field
        }
        throw IllegalStateException()
    }

    private fun roomProcess(env: RoundEnvironment) {
        env.getElementsAnnotatedWith(ARoom::class.java).map { element ->

            val ownerClassSymbol = element.enclosingElement as Symbol.ClassSymbol

            val methodSymbol = ownerClassSymbol.members()
                    .getElements {
                        it is Symbol.MethodSymbol && isSubTypeOfRoomDatabase(it.returnType.asElement())
                    }
                    .firstOrNull() as? Symbol.MethodSymbol
                    ?: throw IllegalStateException("The method whose return type is Retrofit was not found.")

            val kind = getKind(ownerClassSymbol)
            val name = methodSymbol.simpleName()
            val owner = ownerClassSymbol.toString()
            val daoMethods = getMethods(methodSymbol.returnType.asElement())
            RoomInstance(name, owner, kind, daoMethods)
        }.let(Brick::setRoomInstance)
    }

    private fun isSubTypeOfRoomDatabase(typeSymbol: Symbol.TypeSymbol): Boolean {
        val typeName = typeSymbol.toString()
        return when {
            typeName == ROOM_CLASS_NAME -> true
            typeSymbol is Symbol.ClassSymbol -> {
                val superTypeSymbol = typeSymbol.superclass.asElement() ?: return false
                isSubTypeOfRoomDatabase(superTypeSymbol)
            }
            else -> false
        }
    }

    private fun getMethods(typeSymbol: Symbol.TypeSymbol): List<DaoMethod> {
        return typeSymbol.members()
                .getElements {
                    it is ExecutableElement
                }
                .map {
                    it as Symbol.MethodSymbol
                }
                .filter {
                    //方法的返回值是接口或抽象类
                    it.returnType.isInterface || it.returnType.asElement().modifiers.contains(Modifier.ABSTRACT)
                }
                .map {
                    val type = it.returnType.toString()
                    val name = it.simpleName()
                    DaoMethod(type, name)
                }
    }

    companion object {

        private const val ROOM_NAME_SPACE = "androidx.room"
        private const val RETROFIT_NAME_SPACE = "retrofit2.http"
        private const val RETROFIT_CLASS_NAME = "retrofit2.Retrofit"
        private const val ROOM_CLASS_NAME = "androidx.room.RoomDatabase"

    }

}