package com.github.droiddevgeeks.cfsdk.network

import com.github.droiddevgeeks.cfsdk.network.model.SDK
import retrofit2.Call
import retrofit2.http.GET


interface ApiService {
    @GET("assets/update.json")
    fun getUpdates(): Call<List<SDK>>
}