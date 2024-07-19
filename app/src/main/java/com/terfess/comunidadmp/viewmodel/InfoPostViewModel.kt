package com.terfess.comunidadmp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.model.PostRepository

class InfoPostViewModel : ViewModel(), GetPostCallback {
    val postDetails = MutableLiveData<List<Post>>()
    val errorGetPost = MutableLiveData<String>()

    fun getPostDetails(postId:String){
        PostRepository().loadInfoPost(this, postId)
    }

    override fun onGetPosts(posts: List<Post>) {
        postDetails.postValue(posts)
    }

    override fun onError(error: String){
        errorGetPost.postValue(error)
    }
}