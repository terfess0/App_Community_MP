package com.terfess.comunidadmp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.terfess.comunidadmp.R
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.terfess.comunidadmp.databinding.LayoutImageviewBinding

class AdapterHolderViewPostImage(private var dataImages: List<String>, private var context: Context) :
    RecyclerView.Adapter<AdapterHolderViewPostImage.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_imageview, parent, false)
        return Holder(itemView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val currentItem = dataImages[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = dataImages.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding = LayoutImageviewBinding.bind(itemView)


        fun bind(image: String) {
            //load image

            Glide.with(context)
                .load(image)
                .placeholder(R.drawable.predet_image)
                .into(binding.imageView)

            binding.imageView.setOnClickListener {
                // Crear una instancia del BottomSheetDialog con el tema personalizado
                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.layout_img_bottom_sheet, null)

                // Crear y mostrar el AlertDialog
                val alertDialog = AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create()

                // Obtener la referencia de la imagen en el dialogView y cargar la imagen
                val imageView = dialogView.findViewById<PhotoView>(R.id.maxi_image)
                Glide.with(context)
                    .load(image)
                    .placeholder(R.drawable.predet_image)
                    .error(R.drawable.predet_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .into(imageView)

                alertDialog.show()
            }
        }
    }
}



