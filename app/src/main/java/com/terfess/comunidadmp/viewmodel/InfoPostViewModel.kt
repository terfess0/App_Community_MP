package com.terfess.comunidadmp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.terfess.comunidadmp.callbacks.posts.CommentCallback
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.model.Comment
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.model.PostRepository

class InfoPostViewModel : ViewModel(), GetPostCallback, CommentCallback {
    val postDetails = MutableLiveData<List<Post>>()
    val errorGetPost = MutableLiveData<String>()
    val commentsList = MutableLiveData<List<Comment>>()
    val commentAdded = MutableLiveData<Boolean>()

    fun getPostDetails(postId: String) {
        PostRepository().loadInfoPost(this, postId)
    }

    fun loadComments(postId: String) {
        PostRepository().loadComments(this, postId)
    }

    fun addComment(postId: String, comment: Comment) {
        PostRepository().addComment(this, postId, comment)
    }

    fun deleteComment(postId: String, commentId: String) {
        PostRepository().deleteComment(this, postId, commentId)
    }

    override fun onGetPosts(posts: List<Post>, lastDoc: DocumentSnapshot?) {
        postDetails.postValue(posts)
    }

    override fun onCommentsLoaded(comments: List<Comment>) {
        commentsList.postValue(comments)
    }

    override fun onCommentAdded() {
        commentAdded.postValue(true)
    }

    override fun onError(error: String) {
        errorGetPost.postValue(error)
    }
}