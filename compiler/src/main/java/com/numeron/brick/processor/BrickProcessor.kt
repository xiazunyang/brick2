package com.numeron.brick.processor

import com.bennyhuo.aptutils.AptContext
import com.numeron.brick.annotation.Provide
import com.numeron.brick.annotation.RetrofitInstance
import com.numeron.brick.annotation.RoomInstance
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

class BrickProcessor : AbstractProcessor() {

    private val supportedAnnotations = setOf(RetrofitInstance::class.java, RoomInstance::class.java, Provide::class.java)

    override fun init(processingEnv: ProcessingEnvironment) {
        AptContext.init(processingEnv)
        super.init(processingEnv)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        ProviderProcessor().process(roundEnv)
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return supportedAnnotations.mapTo(HashSet(), Class<*>::getCanonicalName)
    }

}