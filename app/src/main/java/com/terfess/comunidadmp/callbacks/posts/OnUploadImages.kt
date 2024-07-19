package com.terfess.comunidadmp.callbacks.posts

interface OnUploadImages {
    fun onUploadSuccess(references:List<String>)
    fun onErrorUploadPostImageStorage(error:String)
}