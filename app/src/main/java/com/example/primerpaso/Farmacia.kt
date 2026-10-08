package com.example.primerpaso

data class Farmacia(
    val fecha_raw: String,
    val fecha_iso: String,
    val dia_semana: String,
    val nombre: String,
    val direccion: String,
    val imagen: String,
    val telefono: String?  // ← con ? para aceptar null
)