package com.terfess.comunidadmp.callbacks.posts

interface FavoritesFirestore {
    fun onRemoveFavorite(result: String)
    fun onErrorRemove(error: String)
    fun onAddFavorite(result: String)
    fun onErrorAdd(error: String)
}