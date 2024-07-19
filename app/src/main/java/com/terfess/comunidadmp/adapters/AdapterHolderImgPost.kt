package com.terfess.comunidadmp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.terfess.comunidadmp.R
import android.view.View
import com.bumptech.glide.Glide
import com.terfess.comunidadmp.databinding.LayoutImageContPostBinding
import com.terfess.comunidadmp.model.ImageSelected

class AdapterHolderImgPost(private var dataImages: MutableList<ImageSelected>) :
    RecyclerView.Adapter<AdapterHolderImgPost.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_image_cont_post, parent, false)
        return Holder(itemView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val currentItem = dataImages[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = dataImages.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding = LayoutImageContPostBinding.bind(itemView)


        fun bind(image: ImageSelected) {
            //load image
            Glide.with(itemView.context)
                .load(image.imageLink)
                .placeholder(R.drawable.predet_image)
                .into(binding.image)


            binding.btnDelete.setOnClickListener {
                //delete image selected of list
                val idcurrentImage = image.idPhoto

                val currentPhoto = dataImages.indexOfFirst {
                    it.idPhoto == idcurrentImage
                }

                if (currentPhoto != -1){
                    dataImages.removeAt(currentPhoto)
                    notifyDataSetChanged()
                }else{
                    Log.e("AdapterImagesPost","There are not photo in list to delete")
                }

            }
        }
    }
    fun addImage(image:ImageSelected){
        dataImages.add(image)
        notifyDataSetChanged()
    }

}

