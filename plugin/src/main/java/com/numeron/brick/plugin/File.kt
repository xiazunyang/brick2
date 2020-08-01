package com.numeron.brick.plugin

import java.io.File

fun File.forEachFileRecurse(block: (File) -> Unit) {
    listFiles()?.forEach {
        block(it)
        it.forEachFileRecurse(block)
    }
}