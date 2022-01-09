package com.safacet.carparkingapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.safacet.carparkingapp.utils.HandlePicture
import java.io.File

class PhotosAdapter(
    private val context: Context,
    var photos: MutableList<String>? = null,
    private val pictureClickListener: PictureClickListener
): RecyclerView.Adapter<PhotosAdapter.ViewHolder>() {

    interface PictureClickListener {
        fun onPictureClicked(bitmap: Bitmap, pos: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotosAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.photo_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotosAdapter.ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return photos?.size ?: 0
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val ivPhotoTaken = itemView.findViewById<ImageView>(R.id.ivPhotoTaken)
            if (photos != null ) {
                val photoPath = photos!![position]
                val file = File(photoPath)
                val uri = Uri.fromFile(file)
                val bitmap = HandlePicture().handleSamplingAndRotationBitmap(context, uri)
                bitmap?.let {
                    ivPhotoTaken.setImageBitmap(bitmap)
                    ivPhotoTaken.setOnClickListener {
                        pictureClickListener.onPictureClicked(bitmap, position)
                    }
                }
            }
        }
    }
}
