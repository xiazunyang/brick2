package com.numeron.brick.processor

import com.google.auto.common.SuperficialValidation
import com.numeron.brick.annotation.Provide
import com.sun.tools.javac.code.Symbol
import javax.annotation.processing.Filer
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class ProvideProcessor {

    fun process(env: RoundEnvironment, filer: Filer) {
        env.getElementsAnnotatedWith(Provide::class.java)
                .filter {
                    SuperficialValidation.validateElement(it)
                }
                .filter {
                    it.kind == ElementKind.CLASS
                }
                .mapNotNull(TypeElement::class.java::cast)
                .forEach { typeElement ->
                    val classSymbol = typeElement as Symbol.ClassSymbol
                    classSymbol.members().elements
                            .first(Symbol::isConstructor)
                            .let {
                                val methodSymbol = it as Symbol.MethodSymbol
                                ProvideGenerator(classSymbol, methodSymbol).generate(filer)
                            }
                }
    }

}