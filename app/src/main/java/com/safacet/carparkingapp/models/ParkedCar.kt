package com.safacet.carparkingapp.models

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

@Entity(tableName = "parked_car")
data class ParkedCar(
   @PrimaryKey var time: Long,
   var latitude: Double? = null,
   var longitude: Double? = null,
   var pictures: MutableList<String>? = null,
   var title: String? = null,
   var description: String? = null,
   var address: String? = null
)

class Converters {

   @TypeConverter
   fun fromString(value: String?): MutableList<String>? {
      val type = object: TypeToken<MutableList<String>>() {}.type
      value?.let {
         return Gson().fromJson(value, type)
      }
      return null
   }

   @TypeConverter
   fun fromMutableList(value: MutableList<String>?): String {
      val gson = Gson()
      return gson.toJson(value)
   }
}

