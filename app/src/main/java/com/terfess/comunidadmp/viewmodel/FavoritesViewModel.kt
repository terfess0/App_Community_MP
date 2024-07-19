package com.terfess.comunidadmp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.terfess.comunidadmp.callbacks.posts.FavoritesFirestore
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.model.FavoritesProvider
import com.terfess.comunidadmp.model.Post

class FavoritesViewModel : ViewModel(), GetPostCallback, FavoritesFirestore {
    val postsFavorites = MutableLiveData<List<Post>>()
    val resultRemoveFavorite = MutableLiveData<String>()

    fun getFavorites() {
        // request favorites
        FavoritesProvider(this).getUserFavorites()
    }

    fun removeFavorite(userEmail: String, idPost: String) {
        FavoritesProvider(this).removeFavorite(this, userEmail, idPost)
    }

    override fun onGetPosts(posts: List<Post>) {
        // update favorites for the view
        postsFavorites.postValue(posts)
    }

    override fun onError(error: String) {
        Log.e("VMFavorites", "Algo salio mal en viewmodel favorites :: $error")
    }

    override fun onRemoveFavorite(result: String) {
        resultRemoveFavorite.postValue(result)
    }

    override fun onErrorRemove(error: String) {
        resultRemoveFavorite.postValue(error)
        Log.e("VMFavorites", "Error remove favorite: $error")
    }

    override fun onAddFavorite(result: String) {
        Log.i("VMFavorites", "Favorite added")
    }

    override fun onErrorAdd(error: String) {
        Log.e("VMFavorites", "Error remove favorite: $error")
    }


}