package com.numeron.brick.processor

import com.bennyhuo.aptutils.AptContext
import com.bennyhuo.aptutils.types.asKotlinTypeName
import com.bennyhuo.aptutils.types.packageName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type

class ProvideGenerator(private val classSymbol: Symbol.ClassSymbol, methodSymbol: Symbol.MethodSymbol) {

    private val ktClassName = classSymbol.asType().asKotlinTypeName()

    private val parameters: List<ParameterSpec>

    private val propertiesSpec: List<PropertySpec>

    init {
        methodSymbol.params()
                .map {
                    val name = it.name.toString()
                    val type = asKotlinKnownTypeName(it.type)

                    val parameterSpec = ParameterSpec.builder(name, type).build()

                    val propertySpec = PropertySpec.builder(name, type)
                            .addModifiers(KModifier.PRIVATE)
                            .initializer(name)
                            .build()
                    propertySpec to parameterSpec
                }
                .unzip()
                .let { (propertiesSpec, parameters) ->
                    this.propertiesSpec = propertiesSpec
                    this.parameters = parameters
                }
    }

    fun generate() {

        val packageName = classSymbol.packageName()

        val fileName = classSymbol.simpleName.toString() + 's'

        FileSpec.builder(packageName, fileName)
                .addType(generateFactoryClass())        //ViewModelFactory
                .addType(generateLazyClass())           //LazyViewModel
                .addFunction(generateLazyFunction())    //kotlin lazy方法
                .addFunction(generateProvideFunction()) //java provide方法
                .build()
                .writeTo(AptContext.filer)
    }

    private fun generateLazyFunction(): FunSpec {

        val className = classSymbol.simpleName.toString()

        val lazyParameterizedTypeName = LAZY_INTERFACE_TYPE_NAME.parameterizedBy(ktClassName)

        return FunSpec.builder("lazy$className")
                .receiver(VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addParameters(parameters)
                .returns(lazyParameterizedTypeName)
                .addStatement("return Lazy$className(this, ${parameters.joinToString(transform = ParameterSpec::name)})")
                .build()
    }

    private fun generateLazyClass(): TypeSpec {
        val simpleName = classSymbol.simpleName.toString()

        val lazyClassName = "Lazy$simpleName"

        val constructorFunSpec = FunSpec.constructorBuilder()
                .addParameter("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addParameters(parameters)
                .build()

        val valueGetterFunSpec = FunSpec.getterBuilder()
                .beginControlFlow("if(_value == null)")
                .addStatement("_value = provide(owner, ${parameters.joinToString(transform = ParameterSpec::name)})")
                .endControlFlow()
                .addStatement("return _value!!")
                .build()

        val valuePropertySpec = PropertySpec.builder("value", ktClassName)
                .addModifiers(KModifier.OVERRIDE)
                .getter(valueGetterFunSpec)
                .build()

        val valuePrivatePropertySpec = PropertySpec.builder("_value", ktClassName.copy(true))
                .addModifiers(KModifier.PRIVATE)
                .initializer("null")
                .mutable()
                .build()

        val isInitializedFunSpec = FunSpec.builder("isInitialized")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Boolean::class.java)
                .addStatement("return _value != null")
                .build()

        val lazyParameterizedTypeName = LAZY_INTERFACE_TYPE_NAME.parameterizedBy(ktClassName)

        val ownerPropertySpec = PropertySpec.builder("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addModifiers(KModifier.PRIVATE)
                .initializer("owner")
                .build()

        return TypeSpec.classBuilder(lazyClassName)
                .addProperty(valuePrivatePropertySpec)
                .addProperty(valuePropertySpec)
                .addFunction(isInitializedFunSpec)
                .primaryConstructor(constructorFunSpec)
                .addProperty(ownerPropertySpec)
                .addProperties(propertiesSpec)
                .addSuperinterface(lazyParameterizedTypeName)
                .build()
    }

    private fun generateProvideFunction(): FunSpec {
        return FunSpec.builder("provide")
                .addParameter("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addParameters(parameters)
                .addStatement("val factory = ${classSymbol.simpleName}Factory(${parameters.joinToString(transform = ParameterSpec::name)})")
                .addStatement("return %T(owner, factory).get(%T::class.java)", VIEW_MODEL_PROVIDER_TYPE_NAME, ktClassName)
                .returns(ktClassName)
                .build()
    }

    private fun generateFactoryClass(): TypeSpec {

        val suppressAnnotationSpec = AnnotationSpec.builder(Suppress::class.java)
                .addMember("%S", "UNCHECKED_CAST")
                .build()

        val className = classSymbol.simpleName.toString()

        val factoryClassName = className + "Factory"

        val typeVariableName = TypeVariableName("VM", VIEW_MODEL_TYPE_NAME)

        val parameterizedTypeName = CLASS_TYPE_NAME.parameterizedBy(typeVariableName)

        val constructorSpec = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()

        val createFunSpec = FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("clazz", parameterizedTypeName)
                .addAnnotation(suppressAnnotationSpec)
                .addTypeVariable(typeVariableName)
                .returns(typeVariableName)
                .addStatement("return ${className}(${parameters.joinToString(transform = ParameterSpec::name)}) as VM")
                .build()

        return TypeSpec.classBuilder(factoryClassName)
                .addSuperinterface(VIEW_MODEL_FACTORY_INTERFACE_TYPE_NAME)
                .addProperties(propertiesSpec)
                .primaryConstructor(constructorSpec)
                .addFunction(createFunSpec)
                .build()
    }

    companion object {

        private val CLASS_TYPE_NAME = Class::class.java.asClassName()
        private val LAZY_INTERFACE_TYPE_NAME = ClassName("kotlin", "Lazy")
        private val VIEW_MODEL_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModel")
        private val VIEW_MODEL_PROVIDER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider")
        private val VIEW_MODEL_STORE_OWNER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelStoreOwner")
        private val VIEW_MODEL_FACTORY_INTERFACE_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider", "Factory")

        private val FUNCTION_MAPPER = mapOf(
                "kotlin.jvm.functions.Function0" to ClassName("kotlin", "Function0"),
                "kotlin.jvm.functions.Function1" to ClassName("kotlin", "Function1"),
                "kotlin.jvm.functions.Function2" to ClassName("kotlin", "Function2"),
                "kotlin.jvm.functions.Function3" to ClassName("kotlin", "Function3"),
                "kotlin.jvm.functions.Function4" to ClassName("kotlin", "Function4"),
                "kotlin.jvm.functions.Function5" to ClassName("kotlin", "Function5"),
                "kotlin.jvm.functions.Function6" to ClassName("kotlin", "Function6"),
                "kotlin.jvm.functions.Function7" to ClassName("kotlin", "Function7"),
                "kotlin.jvm.functions.Function8" to ClassName("kotlin", "Function8"),
                "kotlin.jvm.functions.Function9" to ClassName("kotlin", "Function9")
        )

        private fun asKotlinKnownTypeName(type: Type): TypeName {
            return if (type is Type.ClassType && type.isParameterized) {
                type.typeArguments.map(::asKotlinKnownTypeName).let {
                    FUNCTION_MAPPER.getValue(type.tsym.toString()).parameterizedBy(it)
                }
            } else type.asKotlinTypeName()
        }

    }

}