package com.numeron.brick.processor

import com.bennyhuo.aptutils.AptContext
import com.numeron.brick.annotation.*
import com.numeron.brick.core.Brick
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic

class BrickProcessor : AbstractProcessor() {

    private var processFlag = true
    private lateinit var moduleName: String
    private lateinit var projectName: String

    private val supportedAnnotations = setOf(
            Inject::class.java,
            Provide::class.java,
            RoomInstance::class.java,
            RetrofitInstance::class.java
    )

    override fun init(processingEnv: ProcessingEnvironment) {
        Companion.processingEnv = processingEnv
        AptContext.init(processingEnv)
        super.init(processingEnv)
        moduleName = processingEnv.options["MODULE_NAME"] ?: ""
        projectName = processingEnv.options["PROJECT_NAME"] ?: ""
        if(moduleName.isEmpty() || projectName.isEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "No configuration annotationProcessorOptions, only supports single module project.")
        }
        Brick.init(projectName, moduleName)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        if (processFlag) {
            InjectProcessor(projectName, moduleName).process(roundEnv)
            ProvideProcessor().process(roundEnv)
            processFlag = !processFlag
        }
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return supportedAnnotations.mapTo(HashSet(), Class<*>::getCanonicalName)
    }

    companion object {

        lateinit var processingEnv: ProcessingEnvironment

    }

}