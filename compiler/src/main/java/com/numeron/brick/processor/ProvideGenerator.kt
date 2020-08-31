package com.numeron.brick.processor

import com.bennyhuo.aptutils.types.asKotlinTypeName
import com.bennyhuo.aptutils.types.packageName
import com.bennyhuo.aptutils.types.simpleName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import me.eugeniomarletti.kotlin.metadata.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.WildcardType

class ProvideGenerator(private val classSymbol: Symbol.ClassSymbol, methodSymbol: Symbol.MethodSymbol) {

    private val ktClassName = classSymbol.asType().asKotlinTypeName()

    private val parameters: List<ParameterSpec>

    private val propertiesSpec: List<PropertySpec>

    init {
        //获取构造参数中的名字与具体类型
        typeMapper.clear()
        val metadata = classSymbol.kotlinMetadata
        if (metadata is KotlinClassMetadata) {
            val classData = metadata.data
            val (nameResolver, classProto) = classData
            classProto.constructorList
                    .single { it.isPrimary }
                    .valueParameterList
                    .forEach { valueParameter ->
                        val name = nameResolver.getString(valueParameter.name)
                        val fqClassName = valueParameter.type.extractFullName(classData).replace("`", "")
                        typeMapper[name] = fqClassName
                    }
        }

        methodSymbol.params()
                .map {
                    val name = it.simpleName()
                    val type = asKotlinKnownTypeName(it)

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

    fun generate(filer: Filer) {

        val packageName = classSymbol.packageName()

        val fileName = classSymbol.simpleName.toString() + 's'

        val jvmNameAnnotation = AnnotationSpec.builder(JvmName::class)
                .addMember("%S", fileName)
                .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                .build()

        FileSpec.builder(packageName, fileName)
                .addFunction(generateLazyFunction())    //kotlin lazy方法
                .addFunction(generateGetFunction())     //java provide方法
                .addType(generateFactoryClass())        //ViewModelFactory
                .addType(generateLazyClass())           //LazyViewModel
                .addAnnotation(jvmNameAnnotation)
                .build()
                .writeTo(filer)
    }

    private fun generateLazyFunction(): FunSpec {

        val className = classSymbol.simpleName.toString()

        val lazyParameterizedTypeName = LAZY_INTERFACE_TYPE_NAME.parameterizedBy(ktClassName)

        val parameters = parameters.joinToString(transform = ParameterSpec::name).let {
            if (it.isEmpty()) it else ", $it"
        }

        return FunSpec.builder("lazy$className")
                .receiver(VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addParameters(this.parameters)
                .returns(lazyParameterizedTypeName)
                .addStatement("return Lazy$className(this$parameters)")
                .build()
    }

    private fun generateLazyClass(): TypeSpec {
        val simpleName = classSymbol.simpleName.toString()

        val lazyClassName = "Lazy$simpleName"

        val constructorFunSpec = FunSpec.constructorBuilder()
                .addParameter("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addParameters(parameters)
                .build()

        val parameters = parameters.joinToString(transform = ParameterSpec::name)

        val valueGetterFunSpec = FunSpec.getterBuilder()
                .beginControlFlow("if(_value == null)")
                .addStatement("_value = owner.get$simpleName(${parameters})")
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
                .addModifiers(KModifier.PRIVATE)
                .addProperty(ownerPropertySpec)
                .addProperties(propertiesSpec)
                .addSuperinterface(lazyParameterizedTypeName)
                .build()
    }

    private fun generateGetFunction(): FunSpec {
        val simpleName = classSymbol.simpleName
        return FunSpec.builder("get$simpleName")
                .addParameters(parameters)
                .receiver(VIEW_MODEL_STORE_OWNER_TYPE_NAME)
                .addStatement("val factory = ${simpleName}Factory(${parameters.joinToString(transform = ParameterSpec::name)})")
                .addStatement("return %T(this, factory).get(%T::class.java)", VIEW_MODEL_PROVIDER_TYPE_NAME, ktClassName)
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
                .addModifiers(KModifier.PRIVATE)
                .primaryConstructor(constructorSpec)
                .addFunction(createFunSpec)
                .build()
    }

    companion object : KotlinMetadataUtils {

        private val CLASS_TYPE_NAME = Class::class.java.asClassName()
        private val LAZY_INTERFACE_TYPE_NAME = ClassName("kotlin", "Lazy")
        private val VIEW_MODEL_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModel")
        private val VIEW_MODEL_PROVIDER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider")
        private val VIEW_MODEL_STORE_OWNER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelStoreOwner")
        private val VIEW_MODEL_FACTORY_INTERFACE_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider", "Factory")

        private const val FUNCTION_PACKAGE = "kotlin.jvm.functions"

        private val typeMapper = mutableMapOf<String, String>()

        private fun asKotlinKnownTypeName(varSymbol: Symbol.VarSymbol): TypeName {
            val varType = varSymbol.type
            val notNull = varSymbol.getAnnotation(NotNull::class.java)
            val nullable = varSymbol.getAnnotation(Nullable::class.java)
            if (varType.isParameterized && varType.toString().startsWith(FUNCTION_PACKAGE)) {
                //将泛型中的参数切割成一个String列表
                val simpleName = varSymbol.simpleName()
                val parameterizedNullState = typeMapper[simpleName]
                        ?.substringAfter('<')
                        ?.substringBeforeLast('>')
                        ?.split(',')
                        ?: emptyList()
                //将类型转成KotlinTypeName，然后根据上面的参数列表判断是否可空
                val typeArguments = varType.typeArguments.mapIndexed { index, type ->
                    val kotlinTypeName = type.asTypeName()
                    val isNullable = parameterizedNullState.getOrNull(index)?.endsWith('?') ?: false
                    kotlinTypeName.copy(nullable = isNullable)
                }
                val returnType = typeArguments.last()
                val params = if (typeArguments.size < 2) emptyArray() else typeArguments.subList(0, typeArguments.size - 1).toTypedArray()
                return LambdaTypeName.get(null, *params, returnType = returnType).copy(nullable = nullable != null && notNull == null)
            }
            return varType.asTypeName().copy(nullable = nullable != null && notNull == null)
        }

        private fun Type.asTypeName(): TypeName {
            val typeMirror = if (this is WildcardType) superBound ?: extendsBound else this
            val kotlinTypeName = typeMirror.asKotlinTypeName()
            return when (kotlinTypeName.toString()) {
                "java.lang.Integer" -> INT
                "java.lang.Double" -> DOUBLE
                "java.lang.Float" -> FLOAT
                "java.lang.Long" -> LONG
                "java.lang.Byte" -> BYTE
                "java.lang.Short" -> SHORT
                else -> kotlinTypeName
            }
        }

        override val processingEnv: ProcessingEnvironment
            get() = BrickProcessor.processingEnv

    }

}