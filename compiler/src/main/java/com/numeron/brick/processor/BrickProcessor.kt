package com.numeron.brick.processor

import com.bennyhuo.aptutils.AptContext
import com.numeron.brick.annotation.Provide
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

class BrickProcessor : AbstractProcessor() {

    private lateinit var filer: Filer

    private val supportedAnnotations = setOf(Provide::class.java)

    override fun init(processingEnv: ProcessingEnvironment) {
        AptContext.init(processingEnv)
        filer = processingEnv.filer
        super.init(processingEnv)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        ProvideProcessor().process(roundEnv, filer)
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return supportedAnnotations.mapTo(HashSet(), Class<*>::getCanonicalName)
    }

}