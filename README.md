# Brick

#### 介绍
辅助android开发者搭建基于JetPack组件构建MVVM框架的注解处理框架。通过注解自动生成ViewModel的Factory类、lazy方法等；支持在项目的任意位置注入ROOM的dao层接口与Retrofit库中的api接口。

#### 特点
`android开发者`可以将`brick`理解为一个轻量级的注入框架，使用非常简单，使用2个注解即可工作。`brick`主要在编译期工作， **不会在`App`运行时产生任何额外的性能消耗** ，并且只有1个注解库会打包到你的`android`工程中，**不用担心体积增大**的问题。

#### 适用范围
1. 使用`androidx`而非support库。
2. 使用`JetPack`的ViewModel组件。
3. 使用Retrofit作为网络请求库。
4. 使用ROOM数据库框架。（可选）
5. 服务端为多端口、多IP的项目。(可选)

#### 引入

1.  在你的android工程的根目录下的build.gradle文件中的适当的位置添加以下代码：
```
buildscript {
    ...
    ext {
        brick_version = '0.2.0'
    }
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.gitee.numeron.brick:plugin:$brick_version"
    }
}

allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
2.  在你的android工程中要启用brick的android模块的build.gradle文件中的适当位置添加以下代码：
```
...
apply plugin: 'kotlin-kapt'
apply plugin: 'brick'
...
dependencies {
    ...
    implementation "com.gitee.numeron.brick:annotation:$brick_version"
    kapt "com.gitee.numeron.brick:compiler:$brick_version"
}
```
#### 使用

 **一、 @Provide注解的使用方法：** 
 1. 在你编写好的ViewModel子类上添加@Provide注解
```
@Provide
class WxAuthorViewModel: ViewModel() {
    ...
}
```
 2. 有3种方式让brick注解处理器开始工作：
* 在Termial终端上输入`greadlew :[ModuleName]:kaptDebugKotlin`运行脚本；
* 在AndroidStudio右侧Gradle扩展栏中依次找到`[PrjectName] -> [ModuneName] -> Tasks -> other -> kaptDebugKotlin`并双击运行脚本；
* `Ctrl + F9`编译整个项目。  
 **以上三种方式任选其一即可运行brick注解处理器。** 
 3.  脚本运行结束后，会生成两个包级方法：
* `lazyWxAuthorViewModel()`扩展方法，在Activity或Fragment中直接调用即可。
* `get()`方法，在不方便使用lazy方法时，可使用此方法获取ViewModel的实例。  
 **注：`lazyWxAuthorViewModel`方法就是对`get()`方法的包装。**   
直接使用生成的方法，即可创建对应的ViewModel实例。

 **二、 @Inject注解的使用方法**   
  
-2.  **(必需)** 在获取`Retrofit`实例的方法上添加`@RetrofitInstance`，如：
```
@RetrofitInstance
val retrofit: Retrofit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(WANDROID_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}

val okHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val logInterceptor = HttpLoggingInterceptor()
    logInterceptor.level = HttpLoggingInterceptor.Level.BODY
    OkHttpClient.Builder()
        .addInterceptor(logInterceptor)
        .callTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
}
```  
 **注：`@RetrofitInstance`注解只能标记在public修饰的val属性上或方法上，val属性上或方法可以在`object 单例`或`companion object`中，也可以是包级属性/方法。**   
-1.  **(可选)** 在获取`RoomDatabase`实例的属性或方法上标记`@RoomInstance`，如：
```
@RoomInstance
val wandroidDatabase: WandroidDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Room.databaseBuilder(CONTEXT, WandroidDatabase::class.java, "wandroid.db")
        .build()
}
```
 **注：`@RoomDatabase`注解只能标记在public修饰的val属性上或方法上，val属性上或方法可以在`object 单例`或`companion object`中，也可以是包级属性/方法。**    
0. 假设已有`Retrofit Api`接口和`WxAuthorRepo`类
```
interface WxAuthorApi {
    @GET("wxarticle/chapters/json  ")
    suspend fun getWxAuthorList(): List<WxAuthor>
}

class WxAuthorRepo {
    ...
}

```
1. 在WxAuthorRepo中添加`lateinit var WxAuthorApi`字段，并用`@Inject`标记：
```
class WxAuthorRepo {

    @Inject
    lateinit var wxAuthorApi: WxAuthorApi

}
```
2. 在ViewModel中创建`lateinit var WxAuthorRepo`字段，并用`@Inject`标记：
```
@Provide
class WxAuthorViewModel: ViewModel() {
    @Inject
    private lateinit var wxAuthorRepo: WxAuthorRepo
}
```
标记后，继续编写业务代码即可，所有被`@Inject`标记的字段，都会在编译期自动获取或创建实例，无需担心它们在何时被赋值。   
 **注：虽然是`lateinit var`修饰的字段，但是不要为任何被`@Inject`标记的字段赋值，这会导致致命的错误。**   
 **注：`@Inject`可以处理的类型只有`Retrofit`的`api`接口和`ROOM`的`dao`接口、以及有1个无参构造的类。**   

 **三、 多服务器或多端口的处理方法：** 
假设有另一个Retrofit api接口，它的访问地址或端口与baseUrl中的不一样，此时，可通过`@Port`和`@Url`注解来设置它们的url或port：
  
1. `@Port`的使用：
```
@Port(1080)
interface ArticleApi {

    @GET("wxarticle/list/{chapterId}/{page}/json")
    suspend fun getArticleList(@Path("chapterId") chapterId: Int, @Path("page") page: Int): Paged<Article>

}
```
添加此注解后，brick会在编译期根据`@RetrofitInstance`注解标记的`Retrofit`实例和`@Port`的端口号，重新创建一个`Retrofit`实例，并使用新的`Retrofit`实例创建`ArticleApi`的实例。  
2. `@Url`的使用：
```
@Url("http://www.wanandroid.com:1080/")
interface ArticleApi {

    @GET("wxarticle/list/{chapterId}/{page}/json")
    suspend fun getArticleList(@Path("chapterId") chapterId: Int, @Path("page") page: Int): Paged<Article>

}
```
与`@Port`的使用基本一致，实现的原理也是一样的。