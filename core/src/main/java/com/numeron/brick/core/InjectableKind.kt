package com.numeron.brick.core

enum class InjectableKind {

    Class,      //需要通过类的构造来创建对象

    Companion,  //伴生类

    Object,     //object修饰的单例

    Method,     //无参的方法

    Getter,     //属性的getter

    Field,      //一个字段，一般是私有字段

}