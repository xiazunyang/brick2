package com.numeron.brick.core

data class Inject(

        val variableName: String,

        val variableType: String,

        val owner: String,

        val kind: InjectKind

)