package com.example.primerpaso

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("api/farmacias_turno_android")
    suspend fun obtenerFarmacias(): RespuestaFarmacias

    @GET("api/hybrid-search-android")
    suspend fun buscar(
        @Query("q")         query: String,
        @Query("lat")       lat: String       = "-31.5517",
        @Query("lng")       lng: String       = "-63.1333",
        @Query("localidad") localidad: String = "Villa del Rosario"
    ): RespuestaBusqueda
}