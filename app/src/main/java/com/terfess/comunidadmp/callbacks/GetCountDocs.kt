package com.terfess.comunidadmp.callbacks

interface GetCountDocs {
    fun AtSucces(count: Int)
    fun errorGetCount(error: String)
}