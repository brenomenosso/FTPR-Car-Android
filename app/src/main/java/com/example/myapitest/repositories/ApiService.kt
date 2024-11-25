package com.example.myapitest.repositories

import com.example.myapitest.models.Car
import com.example.myapitest.models.RetrieveCar
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("car")
    suspend fun listCars(): List<Car>

    @POST("car")
    suspend fun createCar(@Body car: Car): Car

    @GET("car/{id}")
    suspend fun retrieveCar(@Path("id") id: String): RetrieveCar

    @DELETE("car/{id}")
    suspend fun destroyCar(@Path("id") id: String)

    @PATCH("car/{id}")
    suspend fun partialUpdateCar(@Path("id") id: String, @Body car: Car): Car


}