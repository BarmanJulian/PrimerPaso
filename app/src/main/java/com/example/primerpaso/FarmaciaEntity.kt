package com.example.primerpaso

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room: representa UNA FILA de la tabla "farmacias".
 *
 * Es distinta del DTO [Farmacia] a propósito:
 *  - [Farmacia] es el formato del JSON que manda la API (lo usa Retrofit/Gson).
 *  - [FarmaciaEntity] es el formato de la base de datos local (lo usa Room).
 * Si mañana la API cambia el JSON, solo se toca el DTO y el mapeo; la base queda igual.
 */
@Entity(tableName = "farmacias")
data class FarmaciaEntity(
    // La API no manda un id, así que armamos uno propio: fecha de turno + nombre.
    // Con esto, volver a guardar la misma farmacia la REEMPLAZA en vez de duplicarla.
    @PrimaryKey val id: String,
    // Lugar que ocupaba en la lista de la API, para mostrarlas en el mismo orden.
    val posicion: Int,
    val fechaRaw: String,
    val fechaIso: String,
    val diaSemana: String,
    val nombre: String,
    val direccion: String,
    val imagen: String,
    val telefono: String?,
    // Momento (en milisegundos) en que se guardó este dato. Sirve para saber qué tan viejo es.
    val fechaActualizacion: Long
)

// ── Mapeo DTO (API) -> Entity (base de datos) ───────────────────────────────
fun Farmacia.toEntity(posicion: Int, ahora: Long = System.currentTimeMillis()) = FarmaciaEntity(
    id                 = "$fecha_iso|$nombre",
    posicion           = posicion,
    fechaRaw           = fecha_raw,
    fechaIso           = fecha_iso,
    diaSemana          = dia_semana,
    nombre             = nombre,
    direccion          = direccion,
    imagen             = imagen,
    telefono           = telefono,
    fechaActualizacion = ahora
)

// ── Mapeo Entity (base de datos) -> modelo de UI ────────────────────────────
// Es la misma conversión que antes se hacía dentro de la pantalla, ahora en un solo lugar.
fun FarmaciaEntity.toUI(): FarmaciaUI {
    val partes = nombre.split(" ")
    val iniciales = if (partes.size >= 2 && partes[0].isNotEmpty() && partes[1].isNotEmpty())
        "${partes[0].first()}${partes[1].first()}"
    else nombre.take(2)

    return FarmaciaUI(
        iniciales = iniciales.uppercase(),
        diaSemana = diaSemana,
        numeroDia = fechaIso.drop(8).take(2),   // "2026-06-15" -> "15"
        mes       = "Jun",
        nombre    = nombre,
        direccion = direccion,
        telefono  = telefono ?: "",
        imagen    = imagen,
        turnoTipo = TurnoTipo.PROXIMO
    )
}
