package com.safacet.carparkingapp

import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.safacet.carparkingapp.models.AppDatabase
import com.safacet.carparkingapp.models.ParkedCar
import com.safacet.carparkingapp.utils.*
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors



class ListSavesActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ListSavesActivity"
    }

    private lateinit var parkedCars: MutableList<ParkedCar>
    private lateinit var rvSavedParks: RecyclerView
    private lateinit var cvEmptyList: CardView
    private lateinit var adViewList: AdView

    private lateinit var adapter: SavedParksAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_saves)

        MobileAds.initialize(this) {}
        adViewList = findViewById(R.id.adViewList)
        val adRequest = AdRequest.Builder().build()
        adViewList.loadAd(adRequest)

        rvSavedParks = findViewById(R.id.rvSavedParks)
        cvEmptyList = findViewById(R.id.cvEmptyList)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.saved_locations_title)
        getSavedParks()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getSavedParks() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val db = Room.databaseBuilder(
                this,
                AppDatabase::class.java,
                "parked-cars-db"
            ).build()

            val parkedCarDao = db.parkedCarDao()
            parkedCars = parkedCarDao.getAll()
            handler.post {
                if (parkedCars.isNullOrEmpty()) {
                    val tv = cvEmptyList.findViewById<TextView>(R.id.tvEmptyText)
                    tv.text = resources.getString(R.string.empty_list)
                } else {
                    cvEmptyList.visibility = View.GONE
                    setupRecyclerView()
                }

            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SavedParksAdapter(this,
            parkedCars,
            object: SavedParksAdapter.OnClickListener{
                override fun onCardClicked(parkedCar: ParkedCar) {
                    onSavedItemClicked(parkedCar)
                }
            })
        val simpleItemTouchHelper = object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                handleDelete(pos)
            }
        }
        val itemTouchHelper = ItemTouchHelper(simpleItemTouchHelper)
        itemTouchHelper.attachToRecyclerView(rvSavedParks)

        rvSavedParks.layoutManager = LinearLayoutManager(this)
        rvSavedParks.adapter = adapter
    }

    private fun handleDelete(pos: Int) {
        AlertDialog.Builder(this@ListSavesActivity)
            .setMessage(resources.getString(R.string.delete_save_message))
            .setNegativeButton(resources.getString(android.R.string.cancel),) {_, _ ->
                adapter.notifyItemChanged(pos)
            }
            .setPositiveButton(resources.getString(android.R.string.ok)) {_, _ ->
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                executor.execute {
                    val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        "parked-cars-db"
                    ).build()

                    val parkedCarDao = db.parkedCarDao()

                    parkedCarDao.delete(parkedCars[pos])

                    handler.post {
                        runOnUiThread {
                                try {
                                    if (parkedCars[pos].pictures != null) {
                                        for (path in parkedCars[pos].pictures!!) {
                                            val pic = File(path)
                                            pic.delete()
                                        }
                                    }
                                    Toast.makeText(this@ListSavesActivity,
                                        resources.getString(R.string.delete_successful), Toast.LENGTH_SHORT).show()
                                    parkedCars.removeAt(pos)
                                    adapter.parkedCars = parkedCars
                                    adapter.notifyItemRemoved(pos)
                                    checkEmptyList()
                                } catch (e: IOException) {
                                    Log.i(MainActivity.TAG, "Exception while file deleting", e)
                                }
                        }
                    }
                }
            }
            .show()

    }

    private fun checkEmptyList() {
        if (parkedCars.isNullOrEmpty()) {
            val tv = cvEmptyList.findViewById<TextView>(R.id.tvEmptyText)
            tv.text = resources.getString(R.string.empty_list)
            cvEmptyList.visibility = View.VISIBLE
        } else {
            cvEmptyList.visibility = View.GONE
        }
    }

    private fun onSavedItemClicked(parkedCar: ParkedCar) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra(TIME, parkedCar.time)
        intent.putExtra(LATITUDE, parkedCar.latitude)
        intent.putExtra(LONGITUDE, parkedCar.longitude)
        if (!parkedCar.pictures.isNullOrEmpty()) {
            intent.putStringArrayListExtra(PICTURES, parkedCar.pictures as ArrayList<String>)
        }
        intent.putExtra(TITLE, parkedCar.title)
        intent.putExtra(DESC, parkedCar.description)
        intent.putExtra(ADDRESS, parkedCar.address)
        startActivity(intent)
    }
}