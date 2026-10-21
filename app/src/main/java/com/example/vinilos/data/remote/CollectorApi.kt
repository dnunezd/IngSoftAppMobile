package com.example.vinilos.data.remote

import retrofit2.http.GET

interface CollectorApi {
    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>
}
