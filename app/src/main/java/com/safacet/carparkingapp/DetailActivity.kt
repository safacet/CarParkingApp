package com.safacet.carparkingapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.safacet.carparkingapp.models.ParkedCar
import com.safacet.carparkingapp.utils.*
import java.io.File

class DetailActivity : AppCompatActivity() {

    private lateinit var clRootDetail: ConstraintLayout
    private lateinit var btnNavigate: Button
    private lateinit var rvPhotosDetail: RecyclerView
    private lateinit var cvEmptyDetail: CardView
    private lateinit var etCommentaryDetail: TextView
    private lateinit var adViewDetail: AdView

    private lateinit var adapter: PhotosAdapter

    private lateinit var parkedCar: ParkedCar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        MobileAds.initialize(this) {}

        adViewDetail = findViewById(R.id.adViewDetail)
        val adRequest = AdRequest.Builder().build()
        adViewDetail.loadAd(adRequest)

        clRootDetail = findViewById(R.id.clRootDetail)
        btnNavigate = findViewById(R.id.btnNavigate)
        rvPhotosDetail = findViewById(R.id.rvPhotosDetail)
        cvEmptyDetail = findViewById(R.id.cvEmptyDetail)
        etCommentaryDetail = findViewById(R.id.etCommentaryDetail)

        parkedCar = ParkedCar(intent.getLongExtra(TIME, 0))
        parkedCar.latitude = intent.getDoubleExtra(LATITUDE, 0.0)
        parkedCar.longitude = intent.getDoubleExtra(LONGITUDE, 0.0)
        parkedCar.pictures = intent.getStringArrayListExtra(PICTURES)
        parkedCar.title = intent.getStringExtra(TITLE)
        parkedCar.description = intent.getStringExtra(DESC)
        parkedCar.address = intent.getStringExtra(ADDRESS)


        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapDetail) as SupportMapFragment
        mapFragment.getMapAsync {
            onMapReady(it)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = parkedCar.title

        val parkAddress = resources.getString(R.string.park_address)
        etCommentaryDetail.text = if (parkedCar.description.isNullOrBlank())
         "$parkAddress ${parkedCar.address}"
        else parkedCar.description

        btnNavigate.setOnClickListener {
            startNavigation()
        }
        setupPictures()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onMapReady(googleMap: GoogleMap) {
        if (parkedCar.longitude == 0.0 || parkedCar.latitude == 0.0) {
            Toast.makeText(this, resources.getString(R.string.could_not_find_saved), Toast.LENGTH_SHORT).show()
            return
        }
        googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        val parkLocation = LatLng(parkedCar.latitude!!, parkedCar.longitude!!)
        val cameraPosition = CameraPosition.Builder().target(parkLocation).zoom(17F).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        googleMap.addMarker(
            MarkerOptions()
                .position(parkLocation)
                .title(parkedCar.title)
                .draggable(false)
        )
    }

    private fun nullRecyclerviewHolder() {
        if (parkedCar.pictures?.size == 0 || parkedCar.pictures == null) {
            rvPhotosDetail.visibility = View.INVISIBLE
            cvEmptyDetail.visibility = View.VISIBLE
        } else {
            rvPhotosDetail.visibility = View.VISIBLE
            cvEmptyDetail.visibility = View.GONE
        }
    }

    private fun setupPictures() {
        adapter = PhotosAdapter(this, parkedCar.pictures, object : PhotosAdapter.PictureClickListener{
            override fun onPictureClicked(bitmap: Bitmap, pos: Int) {
                pictureOnFullSize(bitmap, pos)
            }
        })
        rvPhotosDetail.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPhotosDetail.adapter = adapter
        nullRecyclerviewHolder()
    }

    private fun pictureOnFullSize(bitmap: Bitmap, pos: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.detailed_picture, null)
        val iv = view.findViewById<ImageView>(R.id.ivDetailActivityPicture)
        val builder = AlertDialog.Builder(this)
        iv.setImageBitmap(bitmap)
        builder.setView(view)
        val dialog = builder.create()

        view.setOnClickListener {
            dialog.dismiss()
        }
        builder.show()
    }

    private fun startNavigation() {
        if (parkedCar.latitude == null || parkedCar.longitude == null) {
            Toast.makeText(this, resources.getString(R.string.issue_with_saved), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.opening_maps))
            .setMessage(resources.getString(R.string.directions_for_this_location))
            .setNegativeButton(resources.getString(android.R.string.cancel), null)
            .setPositiveButton(resources.getString(android.R.string.ok)) {_, _ ->
                val intentUri = Uri.parse("google.navigation:q=${parkedCar.latitude},${parkedCar.longitude}&mode=w")
                val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
            .show()
    }
}