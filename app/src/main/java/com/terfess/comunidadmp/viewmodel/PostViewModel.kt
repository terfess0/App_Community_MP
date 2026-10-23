package com.terfess.comunidadmp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.terfess.comunidadmp.callbacks.GetCountDocs
import com.terfess.comunidadmp.callbacks.GetUserCallback
import com.terfess.comunidadmp.callbacks.posts.DeletePost
import com.terfess.comunidadmp.callbacks.posts.FavoritesFirestore
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.callbacks.posts.OnAttachImagesToPost
import com.terfess.comunidadmp.callbacks.posts.OnSetNewMediaPost
import com.terfess.comunidadmp.callbacks.posts.OnUploadImages
import com.terfess.comunidadmp.model.ImageSelected
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.model.PostRepository
import com.terfess.comunidadmp.model.User
import com.terfess.comunidadmp.model.UserProvider

class PostViewModel : ViewModel(),
    GetPostCallback,
    GetUserCallback,
    FavoritesFirestore,
    DeletePost,
    OnSetNewMediaPost,
    OnUploadImages,
    OnAttachImagesToPost,
    GetCountDocs {

    val postViewModel = MutableLiveData<List<Post>>()
    val errorGetPost = MutableLiveData<String>()
    val resultAddFavorite = MutableLiveData<String>()
    val dataUser = MutableLiveData<User>()
    val resultDelPost = MutableLiveData<String>()
    val resultSetPost = MutableLiveData<String>()
    val errorSetPost = MutableLiveData<String>()

    val numTotalPost = MutableLiveData<Int>()
    val currentLastDoc = MutableLiveData<DocumentSnapshot?>()
    val allLoadedPosts = mutableListOf<Post>()
    var isInitialLoaded = false
    val hasNewPostsAvailable = MutableLiveData<Boolean>(false)
    var lastKnownServerTotal = 0
    val postCreationComplete = MutableLiveData<Boolean>()
    private var isRefreshing = false

    fun getPosts(limit: Int?) {
        val lastDoc = if (isRefreshing) null else currentLastDoc.value
        Log.i("VMPost", "getPosts called, currentLastDoc: $lastDoc")
        PostRepository().loadPosts(this, lastDoc, limit ?: 10)
    }

    fun refreshAllPosts() {
        isRefreshing = true
        currentLastDoc.value = null
        allLoadedPosts.clear()
        isInitialLoaded = false
        hasNewPostsAvailable.postValue(false)
        getCountPost()
        getPosts(10)
    }

    override fun onGetPosts(posts: List<Post>, lastDoc: DocumentSnapshot?) {
        isRefreshing = false
        if (lastDoc != null) {
            currentLastDoc.postValue(lastDoc)
        }
        val existingIds = allLoadedPosts.map { it.idPost }.toSet()
        val uniqueNew = posts.filter { it.idPost !in existingIds }
        allLoadedPosts.addAll(uniqueNew)
        isInitialLoaded = true
        postViewModel.postValue(allLoadedPosts.toList())
        Log.i("VMPost", "Posts obtenidos en viewmodel, total acumulados: ${allLoadedPosts.size}")
    }

    override fun onError(error: String) {
        errorGetPost.postValue(error)
        Log.e("VMPost", "Algo ha salido mal al recuperar posts :: $error")
    }


    fun addFavorite(userEmail: String, idPost: String) {
        PostRepository().addFavorite(
            callback = this,
            userEmail = userEmail,
            refNewDocFavorite = idPost
        )
    }

    fun getUserAccount() {
        UserProvider().getUserAccount(this)
    }

    fun getCountPost() {
        PostRepository().getAllDocsCount(this, "posts")
    }

    override fun AtSucces(count: Int) {
        if (isInitialLoaded && lastKnownServerTotal > 0 && count > lastKnownServerTotal) {
            hasNewPostsAvailable.postValue(true)
        }
        lastKnownServerTotal = count
        numTotalPost.postValue(count)
    }

    override fun errorGetCount(error: String) {
        Log.e("VMPost", "Algo ha salido mal al recuperar numero posts :: $error")
    }

    fun deletePostAdmin(postId: String) {
        PostRepository().deletePost(this, postId)
    }

    fun setPost(dataPost: HashMap<String, Any?>) {
        PostRepository().setNewPost(this, dataPost)
    }

    fun uploadImagesPost(images: MutableList<ImageSelected>, context: Context) {
        PostRepository().uploadImagesGetUrl(this, context, images)
    }

    override fun onRemoveFavorite(result: String) {
        TODO("Not yet implemented")
    }

    override fun onErrorRemove(error: String) {
        TODO("Not yet implemented")
    }

    override fun onAddFavorite(result: String) {
        resultAddFavorite.postValue(result)
        Log.i("VMPost", "Favorite added successfully")
    }

    override fun onErrorAdd(error: String) {
        resultAddFavorite.postValue(error)
        Log.e("VMPost", "Error add favorite: $error")
    }

    override fun onGetUser(user: User) {
        dataUser.postValue(user)
    }

    override fun onGetUserError(error: String) {
        Log.e("error", "Algo salio mal al recibir info cuenta de usuario :: $error")
    }

    override fun onDeletePostSuccess(result: String) {
        resultDelPost.postValue(result)
        println("ANUNCIANDO")
    }

    override fun onErrorDeletePostError(error: String) {
        resultDelPost.postValue(error)
    }

    override fun onResult(docReference: String) {
        resultSetPost.postValue(docReference)
        println("Todo salio bien set post $docReference")
    }

    override fun onErrorSetPost(error: String) {
        errorSetPost.postValue("ERROR")
    }

    override fun onUploadSuccess(references: List<String>) {
        //success opload images of post to firebase
        //attach the image references at the post
        PostRepository().attachImagesToPost(this, resultSetPost.value.toString(), references)
    }

    override fun onErrorUploadPostImageStorage(error: String) {
        errorSetPost.postValue("ERROR_IMAGES")
    }

    override fun onAttachImagesSuccess() {
        postCreationComplete.postValue(true)
        Log.i("VMPost", "Imágenes adjuntadas correctamente al post")
    }

    override fun onAttachImgsError() {
        errorSetPost.postValue("ERROR_ATTACH")
    }

}