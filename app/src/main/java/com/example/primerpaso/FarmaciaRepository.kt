package com.example.primerpaso

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/** Resultado de intentar actualizar los datos desde la API. */
sealed interface ResultadoRefresco {
    /** Todo bien (o no hizo falta llamar a la API). */
    data object Ok : ResultadoRefresco
    /** No hay internet. Los datos guardados NO se tocan. */
    data object SinConexion : ResultadoRefresco
    /** La API respondió mal o con datos inválidos. Los datos guardados NO se tocan. */
    data class Fallo(val mensaje: String) : ResultadoRefresco
}

/**
 * Repositorio: único punto de acceso a los datos de farmacias.
 *
 * Room es la ÚNICA FUENTE DE VERDAD:
 *   API  ──(refrescar)──►  Room  ──(Flow)──►  ViewModel  ──►  Pantalla
 * La pantalla nunca lee de la API: solo observa [farmacias], que sale de Room.
 */
class FarmaciaRepository(
    private val db: AppDatabase,
    private val api: ApiService
) {
    private val dao = db.farmaciaDao()

    /** Lo que ve la UI: la lista guardada en Room, que se actualiza sola cuando cambia la tabla. */
    val farmacias: Flow<List<FarmaciaEntity>> = dao.obtenerTodos()

    /**
     * Farmacias cuyo nombre contiene [texto]. El filtro lo resuelve Room con una query LIKE.
     * Si el texto está vacío devuelve todas. Como es un Flow, se vuelve a emitir si la tabla cambia.
     */
    fun buscar(texto: String): Flow<List<FarmaciaEntity>> {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return farmacias
        // En LIKE, % y _ son comodines: los escapamos para que "50%" busque el texto tal cual.
        val escapado = limpio
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return dao.buscarPorNombre(escapado)
    }

    /**
     * Baja los datos de la API y los guarda en Room.
     *
     * @param forzar si es false, solo llama a la API cuando Room está vacío;
     *               si es true, siempre intenta actualizar (botón "Actualizar").
     */
    suspend fun refrescar(forzar: Boolean = false): ResultadoRefresco {
        if (!forzar && dao.contar() > 0) return ResultadoRefresco.Ok

        return try {
            val respuesta = api.obtenerFarmacias()
            // Si la API no trae la lista, es un error: NO hay que borrar lo que ya teníamos.
            val lista = respuesta.farmacias
                ?: return ResultadoRefresco.Fallo("La API no devolvió farmacias")

            val ahora = System.currentTimeMillis()
            val entidades = lista.mapIndexed { indice, dto -> dto.toEntity(indice, ahora) }

            // withTransaction: borrar + insertar es "todo o nada". Si algo falla a la mitad,
            // Room deshace todo y la tabla queda como estaba (nunca queda vacía por error).
            db.withTransaction {
                dao.borrarTodos()
                dao.insertarTodos(entidades)
            }
            ResultadoRefresco.Ok
        } catch (e: CancellationException) {
            throw e   // cancelar una corrutina no es un error: hay que dejarla propagar
        } catch (e: IOException) {
            // Sin internet, timeout, DNS caído... No se borra nada.
            ResultadoRefresco.SinConexion
        } catch (e: Exception) {
            // HttpException (404, 500...), JSON mal formado, etc.
            ResultadoRefresco.Fallo(e.message ?: "Error desconocido")
        }
    }
}
