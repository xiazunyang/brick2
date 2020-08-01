package com.numeron.brick.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.CLASS)
annotation class RetrofitInstance(val value: String = "")