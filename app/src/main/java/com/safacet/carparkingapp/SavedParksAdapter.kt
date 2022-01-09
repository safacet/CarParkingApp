package com.safacet.carparkingapp

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.safacet.carparkingapp.R.drawable.ic_baseline_directions_car_24
import com.safacet.carparkingapp.models.ParkedCar
import com.safacet.carparkingapp.utils.HandlePicture
import com.safacet.carparkingapp.utils.dateFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SavedParksAdapter(
    private val context: Context,
    var parkedCars: MutableList<ParkedCar>,
    private val onClickListener: OnClickListener
): RecyclerView.Adapter<SavedParksAdapter.ViewHolder>() {

    companion object {
        const val TAG = "SavedParksAdapter"
    }

    interface OnClickListener {
        fun onCardClicked(parkedCar: ParkedCar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.saved_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return parkedCars.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            val tvDescription = itemView.findViewById<TextView>(R.id.tvDescription)
            val tvAddress = itemView.findViewById<TextView>(R.id.tvAddress)
            val ivPicture = itemView.findViewById<ImageView>(R.id.ivPicture)
            val cvItemCard = itemView.findViewById<CardView>(R.id.cvItemCard)

            val c = Calendar.getInstance()
            c.timeInMillis = parkedCars[position].time
            tvTime.text = SimpleDateFormat(dateFormat, Locale.getDefault()).format(c.time)
            tvTitle.text = parkedCars[position].title
            tvDescription.text = parkedCars[position].description
            tvAddress.text = parkedCars[position].address

            if (parkedCars[position].pictures != null && parkedCars[position].pictures?.size != 0) {
                val photoPath = parkedCars[position].pictures?.get(0)
                photoPath?.let {
                    val file = File(it)
                    val uri = Uri.fromFile(file)
                    val bitMap = HandlePicture().handleSamplingAndRotationBitmap(context, uri)
                    ivPicture.setImageBitmap(bitMap)
                }
            }
            else ivPicture.setImageBitmap(null)

            cvItemCard.setOnClickListener {
                onClickListener.onCardClicked(parkedCars[position])
            }
        }

    }
}