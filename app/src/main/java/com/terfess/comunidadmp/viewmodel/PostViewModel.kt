package com.terfess.comunidadmp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.terfess.comunidadmp.callbacks.posts.DeletePost
import com.terfess.comunidadmp.callbacks.posts.FavoritesFirestore
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.callbacks.GetUserCallback
import com.terfess.comunidadmp.callbacks.posts.OnAttachImagesToPost
import com.terfess.comunidadmp.callbacks.posts.OnSetNewPost
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
    OnSetNewPost,
    OnUploadImages,
    OnAttachImagesToPost {

    val postViewModel = MutableLiveData<List<Post>>()
    val errorGetPost = MutableLiveData<String>()
    val resultAddFavorite = MutableLiveData<String>()
    val dataUser = MutableLiveData<User>()
    val resultDelPost = MutableLiveData<String>()
    val resultSetPost = MutableLiveData<String>()
    val errorSetPost = MutableLiveData<String>()

    fun getPosts() {
        PostRepository().loadPosts(this)
        Log.i("VMPost", "Posts han sido solicitados desde viewmodel")
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

    fun deletePostAdmin(postId: String) {
        PostRepository().deletePost(this, postId)
    }

    fun setPost(dataPost: HashMap<String, Any?>) {
        PostRepository().setNewPost(this, dataPost)
    }

    fun uploadImagesPost(images: MutableList<ImageSelected>, context: Context) {
        PostRepository().uploadImagesGetUrl(this, context, images)
    }

    override fun onGetPosts(posts: List<Post>) {
        postViewModel.postValue(posts)
        Log.i("VMPost", "Posts obtenidos en viewmodel")
    }

    override fun onError(error: String) {
        errorGetPost.postValue(error)
        Log.e("VMPost", "Algo ha salido mal al recuperar posts :: $error")
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
        println("SUCCESS POST")
    }

    override fun onAttachImgsError() {
        errorSetPost.postValue("ERROR_ATTACH")
    }


}