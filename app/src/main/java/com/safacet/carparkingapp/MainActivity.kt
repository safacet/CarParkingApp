package com.safacet.carparkingapp

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.safacet.carparkingapp.models.AppDatabase
import com.safacet.carparkingapp.models.ParkedCar
import com.safacet.carparkingapp.utils.SingleShotLocationProvider
import com.safacet.carparkingapp.utils.dateFormat
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG= "MainActivity"

    }

    private lateinit var etCommentary: EditText
    private lateinit var rvPhotos: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var ibCalender: ImageButton
    private lateinit var ibAddPhoto: ImageButton
    private lateinit var adViewMain: AdView
    private lateinit var toolbarTitle: TextView
    private lateinit var ibList: ImageButton
    private lateinit var motionLayout: MotionLayout


    private lateinit var adapter: PhotosAdapter
    private lateinit var map: GoogleMap
    private lateinit var parkedCar: ParkedCar

    private lateinit var currentPhotoPath: String

    private var currentLocation: LatLng? = null
    private var marker: Marker? = null
    private val client = OkHttpClient()


    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    private var isCameraPermissionGranted = false


    private val externalPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
        isReadPermissionGranted = permissions[READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
        isWritePermissionGranted = permissions[WRITE_EXTERNAL_STORAGE] ?: isWritePermissionGranted
        isCameraPermissionGranted = permissions[CAMERA] ?: isCameraPermissionGranted
        if (isWritePermissionGranted && isCameraPermissionGranted && isReadPermissionGranted) {
            onAddPhotoClicked()
        } else {
            Toast.makeText(this, resources.getString(R.string.external_permission_request_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        getLocationAndMap()
    }

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            processPicturesTaken()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        etCommentary = findViewById(R.id.etCommentary)
        rvPhotos = findViewById(R.id.rvPhotos)
        btnSave = findViewById(R.id.btnSave)
        ibCalender = findViewById(R.id.ibCalender)
        ibAddPhoto = findViewById(R.id.ibAddPhoto)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        ibList = findViewById(R.id.ibList)
        adViewMain = findViewById(R.id.adViewMain)
        motionLayout = findViewById(R.id.clRoot)

        parkedCar = ParkedCar(Calendar.getInstance().timeInMillis)

        val c = Calendar.getInstance().time
        val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        parkedCar.title = sdf.format(c).toString()
        supportActionBar?.hide()
        toolbarTitle.text = title


        runAfterAnimation()

        adapter = PhotosAdapter(this, parkedCar.pictures, object: PhotosAdapter.PictureClickListener {
            override fun onPictureClicked(bitmap: Bitmap, pos: Int) {
                showPicture(bitmap, pos)
            }

        })
        rvPhotos.adapter = adapter
        rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        ibAddPhoto.setOnClickListener {
            externalPermission()
        }

        ibCalender.setOnClickListener {
            addCalendarEvent()
        }

        btnSave.setOnClickListener {
            onSaveButtonClicked()
        }

        ibList.setOnClickListener {
            val intent = Intent(this, ListSavesActivity::class.java)
            startActivity(intent)
        }
    }



    private fun externalPermission() {
        val isReadPermission = ContextCompat.checkSelfPermission(this,
            READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        val isWritePermission = ContextCompat.checkSelfPermission(this,
            WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        val isCameraPermission = ContextCompat.checkSelfPermission(this,
            CAMERA) == PackageManager.PERMISSION_GRANTED

        val isMinSdkLevel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val permissionRequest = mutableListOf<String>()

        isReadPermissionGranted = isReadPermission
        isWritePermissionGranted = isWritePermission || isMinSdkLevel
        isCameraPermissionGranted = isCameraPermission

        if(!isWritePermissionGranted) {
            permissionRequest.add(WRITE_EXTERNAL_STORAGE)
        }

        if(!isReadPermissionGranted) {
            permissionRequest.add(READ_EXTERNAL_STORAGE)
        }

        if(!isCameraPermissionGranted) {
            permissionRequest.add(CAMERA)
        }

        if(permissionRequest.isNotEmpty()) {
            externalPermissionRequest.launch(permissionRequest.toTypedArray())
        } else {
            onAddPhotoClicked()
        }
    }


    private fun runAfterAnimation() {
        val transitionListener = object : MotionLayout.TransitionListener{
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {}

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {}

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                val adRequest = AdRequest.Builder().build()
                adViewMain.loadAd(adRequest)

                checkLocationPermission()
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {}

        }
        motionLayout.setTransitionListener(transitionListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showPicture(bitmap: Bitmap, position: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_img_preview, null)
        val imageView = view.findViewById<ImageView>(R.id.ivPicDetail)
        val ibDelete = view.findViewById<ImageButton>(R.id.ibDelete)
        val ibClose = view.findViewById<ImageButton>(R.id.ibClose)
        val builder = AlertDialog.Builder(this)
        imageView.setImageBitmap(bitmap)
        builder.setView(view)
        val dialog = builder.create()

        ibDelete.setOnClickListener {
            runOnUiThread {
                if (parkedCar.pictures != null && parkedCar.pictures!!.size > position) {
                    try {
                        val pic = File(parkedCar.pictures!![position])
                        pic.delete()
                    } catch (e: IOException) {
                        Log.i(TAG, "Encountered an error while file deleting", e)
                    }
                }
            }
            parkedCar.pictures?.removeAt(position)
            adapter.photos = parkedCar.pictures
            if (parkedCar.pictures?.size != 0) {
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(0, parkedCar.pictures!!.size)
            } else {
                adapter.notifyDataSetChanged()
            }
            dialog.dismiss()
        }

        ibClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()


    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onSaveButtonClicked() {
        parkedCar.time = Calendar.getInstance().timeInMillis
        parkedCar.title = toolbarTitle.text.toString()
        parkedCar.description = etCommentary.text.toString()
        parkedCar.latitude = currentLocation?.latitude
        parkedCar.longitude = currentLocation?.longitude

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "parked-cars-db"
            )
                .build()

            val parkedCarDao = db.parkedCarDao()
            parkedCarDao.insertAll(parkedCar)
            db.close()
            handler.post {
                Toast.makeText(this, resources.getString(R.string.park_location_saved), Toast.LENGTH_SHORT).show()
                parkedCar.title = null
                parkedCar.description = null
                parkedCar.pictures = null
                etCommentary.text.clear()
                adapter.photos = null
                adapter.notifyDataSetChanged()
            }
        }


    }

    private fun checkLocationPermission() {
        val requestArray = mutableListOf<String>()
        if(ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            requestArray.add(ACCESS_FINE_LOCATION)
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestArray.add(ACCESS_COARSE_LOCATION)
        }

        if(requestArray.isNotEmpty()) {
            locationPermissionRequest.launch(requestArray.toTypedArray())
        } else {
            getLocationAndMap()
        }
    }

    private fun getLocationAndMap() {
        SingleShotLocationProvider().requestSingleUpdate(this, object : SingleShotLocationProvider.LocationCallback {
            override fun onNewLocationAvailable(location: LatLng?) {
                if (location != null) {
                    currentLocation = location
                }
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync {
                    onMapReady(it)
                }
            }
        })
    }

    private fun onMapReady(googleMap: GoogleMap) {
        if (currentLocation == null) {
            currentLocation = LatLng(41.0082, 28.9784)
            Toast.makeText(this, resources.getString(R.string.cold_not_find_location), Toast.LENGTH_LONG).show()
        }
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_TERRAIN

        val cameraPosition = CameraPosition.Builder().target(currentLocation!!).zoom(17F).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), object: GoogleMap.CancelableCallback {
            override fun onCancel() {
            }

            override fun onFinish() {
                map.setOnCameraMoveListener{
                    val mPosition = map.cameraPosition.target
                    currentLocation = mPosition
                    if (marker != null) {
                        marker!!.position = mPosition
                    } else {
                        marker = map.addMarker(
                            MarkerOptions()
                                .position(mPosition)
                                .draggable(false)
                        )
                    }
                }
            }

        })
        marker = map.addMarker(
            MarkerOptions()
                .position(currentLocation!!)
                .title(parkedCar.title)
                .draggable(false)
        )

        map.setOnCameraIdleListener {
            getProvince(currentLocation!!)
        }
    }

    private fun getProvince(position: LatLng) {
        val key = resources.getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${position.latitude},${position.longitude}&key=$key"
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {

                val stringResponse = response.body()?.string()
                if (stringResponse != null) {
                    try {
                        parkedCar.title = JSONObject(stringResponse)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getJSONArray("address_components")
                            .getJSONObject(3)
                            .getString("long_name")
                        parkedCar.address = JSONObject(stringResponse)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getString("formatted_address")
                    } catch (exception: Exception) {
                        val c = Calendar.getInstance().time
                        val df = SimpleDateFormat(dateFormat, Locale.getDefault())
                        parkedCar.title = df.format(c).toString()

                    } finally {
                        runOnUiThread {
                            toolbarTitle.text= parkedCar.title
                        }
                    }
                }

            }

        })
    }

    private fun onAddPhotoClicked() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE). also { takePictureIntent ->
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoUri: Uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    cameraActivityLauncher.launch(takePictureIntent)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun processPicturesTaken() {
        if (parkedCar.pictures == null) {
            parkedCar.pictures = mutableListOf(currentPhotoPath)
        } else {
            parkedCar.pictures!!.add(currentPhotoPath)
        }

        adapter.photos = parkedCar.pictures
        if(parkedCar.pictures!!.size ==1) {
            adapter.notifyDataSetChanged()
        } else {
            adapter.notifyItemInserted(parkedCar.pictures!!.size-1)
        }


    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) as String
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }

    }

    private fun addCalendarEvent() {
        parkedCar.description = etCommentary.text.toString()
        val cal = Calendar.getInstance()
        val intent = Intent(Intent.ACTION_EDIT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra("beginTime", cal.timeInMillis)
        intent.putExtra("allDay", true)
        intent.putExtra("endTime", cal.timeInMillis + 60*60*1000)
        intent.putExtra("title", "Parked Vehicle: ${parkedCar.title}")
        intent.putExtra("eventLocation", parkedCar.address)
        intent.putExtra("description", "Park location description: ${parkedCar.description}")
        startActivity(intent)
    }
}