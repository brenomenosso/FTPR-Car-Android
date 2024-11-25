package com.example.myapitest

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.databinding.ActivityCarCreateBinding
import com.example.myapitest.models.Car
import com.example.myapitest.models.Place
import com.example.myapitest.repositories.Result
import com.example.myapitest.repositories.RetrofitClient
import com.example.myapitest.repositories.safeApiCall
import com.example.myapitest.utils.firebaseLogout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CarCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarCreateBinding
    private lateinit var userLocation: Place

    private lateinit var imageUri: Uri
    private var imageFile: File? = null
    private var imageFirebaseUri = ""

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        binding.btConfirm.isEnabled = false
        binding.takePicture.isEnabled = false
        if (result.resultCode == Activity.RESULT_OK) {
            binding.createImage.setImageURI(imageUri)
            binding.createImage.visibility = View.VISIBLE
            uploadImageToFirebase()
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
        requestLocationPermission()
    }

    private fun uploadImageToFirebase() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(imageFile!!.path)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(
                this,
                getString(R.string.failed_to_upload),
                Toast.LENGTH_LONG
            ).show()
        }.addOnSuccessListener {
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                imageFirebaseUri = uri.toString()
            }

            binding.takePicture.isEnabled = true
            binding.btConfirm.isEnabled = true
        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.logoutButton -> {
            val newIntent = firebaseLogout(this)
            startActivity(newIntent)
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun requestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {
                Log.d("LOCATION", "Permissão garantida")
                getLastLocation()
            } else {
                Log.d("LOCATION", "Permissão não concedida")
                Toast.makeText(
                    this,
                    getString(R.string.could_not_obtain_location_permission),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Log.d("LOCATION", "Iniciando a busca pela permissão")
        checkLocationPermissionAndRequest()
    }

    private fun checkLocationPermissionAndRequest() {
        when {
            ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED -> {
                    getLastLocation()
            }
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
            shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION) -> {
                locationPermissionLauncher.launch(ACCESS_COARSE_LOCATION)
            }
            else -> {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        )
        {
            requestLocationPermission()
            return
        }
        fusedLocationClient.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful) {
                val location = task.result
                userLocation = Place(
                    lat = location.latitude.toFloat(),
                    long = location.longitude.toFloat()
                )

            } else {
                Log.d("LOCATION", "A localização não foi obtida ${task.exception}")
                Toast.makeText(
                    this,
                    getString(R.string.unknown_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setUpView() {
        supportActionBar?.title = getString(R.string.cereate_new_vehicle)
        binding.btConfirm.setOnClickListener {
            btConfirmOnClick()
        }

        binding.takePicture.setOnClickListener {
            btTakePictureOnClick()
        }
    }

    private fun btTakePictureOnClick() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), CAMERA_REQUEST_CODE)
    }

    private fun btConfirmOnClick() {
        if (!verifyValidField(binding.etCreateName)) {
            return
        }

        if (!verifyValidField(binding.etCreateYear)) {
            return
        }

        if (!verifyValidField(binding.etCreateLicense)) {
            return
        }

        if (imageFile == null) {
            Toast.makeText(
                this,
                getString(R.string.no_picture_selected),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        getLastLocation()


        val car = Car(
            id= UUID.randomUUID().toString(),
            name= binding.etCreateName.text.toString(),
            year = binding.etCreateYear.text.toString(),
            licence = binding.etCreateLicense.text.toString(),
            imageUrl = imageFirebaseUri,
            place = userLocation
        )
        Log.d("CAR", car.toString())
        CoroutineScope(Dispatchers.IO).launch {

            val result = safeApiCall { RetrofitClient.apiService.createCar(car) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarCreateActivity,
                            getString(R.string.unknown_error),
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("POST", result.message)
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@CarCreateActivity,
                            getString(R.string.successfully_create_vehicle),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        startActivity(CarDetailActivity.newIntent(this@CarCreateActivity, car.id))
                    }
                }
            }
        }
    }

    private fun verifyValidField(field: TextInputEditText): Boolean {
        if (field.text.isNullOrEmpty()) {
            field.requestFocus()
            Toast.makeText(
                this,
                getString(R.string.fill_empty_fields),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }

    companion object {

        private const val CAMERA_REQUEST_CODE = 100

        fun newIntent(context: Context) = Intent(context, CarCreateActivity::class.java)
    }
}