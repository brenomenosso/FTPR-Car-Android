package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.models.Car
import com.example.myapitest.repositories.Result
import com.example.myapitest.repositories.RetrofitClient
import com.example.myapitest.repositories.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.example.myapitest.utils.firebaseLogout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var car: Car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        retrieveCar()
        setupView()
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

    private fun setupView() {
        supportActionBar?.title = getString(R.string.vehicle_details)
        binding.destroyCar.setOnClickListener {
            destroyOnClick()
        }
        binding.updateCar.setOnClickListener {
            updateOnClick()
        }
    }

    private fun updateOnClick() {

        if (!verifyValidField(binding.etName)) {
            return
        }
        if (!verifyValidField(binding.etYear)){
            return
        }
        if (!verifyValidField(binding.etLicense)) {
            return
        }

        car = car.copy(
            name = binding.etName.text.toString(),
            year = binding.etYear.text.toString(),
            licence = binding.etLicense.text.toString()
        )


        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.partialUpdateCar(
                    car.id,
                    car
                )
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            getString(R.string.could_not_update_vehicle),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            getString(R.string.successfully_updated_vehicle),
                            Toast.LENGTH_LONG
                        ).show()
                        recreate()
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

    private fun destroyOnClick() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.destroyCar(car.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            getString(R.string.could_not_destroy_vehicle),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            getString(R.string.successfully_deleted_vehicle),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun retrieveCar() {
        val carId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.retrieveCar(carId) }

            withContext(Dispatchers.Main) {

                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            getString(R.string.could_not_retrieve_vehicle_data),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                    is Result.Success -> {
                        car = result.data.value
                        handleSuccess()
                    }
                }
            }
        }
    }

    private fun handleSuccess() {
        binding.etName.setText(car.name)
        binding.etLicense.setText( car.licence)
        binding.etYear.setText(car.year)
        binding.place.text = getString(
            R.string.lat_long_detail,
            car.place.lat.toString(),
            car.place.long.toString()
        )
        binding.imageView.loadUrl(car.imageUrl)
    }


    companion object {
        private const val ARG_ID = "ARG_ID"

        fun newIntent(
            context: Context,
            carId: String
        ) = Intent(
            context,
            CarDetailActivity::class.java
        ).apply {
            putExtra(ARG_ID, carId)
        }
    }

}