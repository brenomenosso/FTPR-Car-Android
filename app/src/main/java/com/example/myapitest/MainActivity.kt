package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.adapters.CarItemAdapter
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.models.Car
import com.example.myapitest.repositories.RetrofitClient
import com.example.myapitest.repositories.Result
import com.example.myapitest.repositories.safeApiCall
import com.example.myapitest.utils.firebaseLogout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    private fun setupView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            fetchItems()
        }
        binding.addCta.setOnClickListener {
            startActivity(CarCreateActivity.newIntent(this))
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

    private fun fetchItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.listCars() }
            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is Result.Success -> handleFetchSuccess(result.data)
                }
            }
        }
    }

    private fun handleFetchSuccess(data: List<Car>) {
        Log.d("API", data.toString())
        val adapter = CarItemAdapter(data) {
            startActivity(CarDetailActivity.newIntent(this, it.id))
        }
        binding.recyclerView.adapter = adapter
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
