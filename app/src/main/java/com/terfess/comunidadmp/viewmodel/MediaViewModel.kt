package com.terfess.comunidadmp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.terfess.comunidadmp.callbacks.GetCountDocs
import com.terfess.comunidadmp.callbacks.IncrementFieldCallback
import com.terfess.comunidadmp.callbacks.posts.DeletePost
import com.terfess.comunidadmp.callbacks.posts.OnSetNewMediaPost
import com.terfess.comunidadmp.callbacks.postsMedias.GetMediaPostCallback
import com.terfess.comunidadmp.model.PostMedia
import com.terfess.comunidadmp.model.PostRepository
import com.terfess.comunidadmp.model.PostsMediaProvider
import java.io.Closeable

class MediaViewModel : ViewModel(),
    GetMediaPostCallback,
    OnSetNewMediaPost,
    IncrementFieldCallback,
    GetCountDocs,
    DeletePost{
    val postsMediasList = MutableLiveData<MutableList<PostMedia>>()
    val postsGetError = MutableLiveData<String>()
    val resultAddMediaPost = MutableLiveData<String>()
    val succesLike = MutableLiveData<Boolean>()
    val countMediaPosts = MutableLiveData<Int>()
    val resultDelPostMedia = MutableLiveData<String>()

    private val providerInst = PostsMediaProvider()

    val currentLastMediaDoc = MutableLiveData<DocumentSnapshot>()

    fun getMediaPosts(limit: Int?) {
        PostsMediaProvider().loadPosts(this, currentLastMediaDoc.value, limit ?: 10)
        Log.i("MediaPostLog", "MediaPosts han sido solicitados desde viewmodel")
    }

    override fun onGetPosts(posts: MutableList<PostMedia>, lastDoc: DocumentSnapshot?) {
        postsMediasList.postValue(posts)
    }

    override fun onError(error: String) {
        postsGetError.postValue(error)
    }

    fun setMediaPost(post: HashMap<String, Any?>) {
        providerInst.setPost(this, post)
    }

    override fun onResult(docReference: String) {
        resultAddMediaPost.postValue(docReference)
    }

    override fun onErrorSetPost(error: String) {
        resultAddMediaPost.postValue(error)
    }

    fun likeMediaPost(postId: String) {
        providerInst.setCounterLikesMediaPost(this, postId, "ADD")
    }

    override fun onSuccesIncrement() {
        succesLike.postValue(true)
    }

    fun getCountMediaPosts() {
        PostRepository().getAllDocsCount(this, "mediaPosts")
    }

    override fun AtSucces(count: Int) {
        countMediaPosts.postValue(count)
    }

    override fun errorGetCount(error: String) {
        countMediaPosts.postValue(0)
    }

    fun deleteMediaPost(idPost: String) {
        providerInst.deletePost(this, idPost)
    }

    override fun onDeletePostSuccess(result: String) {
        resultDelPostMedia.postValue(result)
    }

    override fun onErrorDeletePostError(error: String) {
        resultDelPostMedia.postValue(error)
    }
}