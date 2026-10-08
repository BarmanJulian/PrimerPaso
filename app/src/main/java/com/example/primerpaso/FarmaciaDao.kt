package com.example.primerpaso

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): acá se declaran las operaciones sobre la tabla "farmacias".
 * Room genera el código real en tiempo de compilación (por eso se necesita KSP).
 *
 * Dos reglas que se cumplen en todo el DAO:
 *  - Las lecturas devuelven [Flow]: Room vuelve a emitir la lista cada vez que la tabla cambia.
 *  - Las escrituras son `suspend`: Room las corre fuera del hilo principal.
 */
@Dao
interface FarmaciaDao {

    /** Todas las farmacias, en el orden en que las mandó la API. La UI observa este Flow. */
    @Query("SELECT * FROM farmacias ORDER BY posicion ASC")
    fun obtenerTodos(): Flow<List<FarmaciaEntity>>

    /** Inserta la lista; si ya existe una con el mismo id, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(farmacias: List<FarmaciaEntity>)

    @Update
    suspend fun actualizar(farmacia: FarmaciaEntity)

    @Delete
    suspend fun eliminar(farmacia: FarmaciaEntity)

    /**
     * Búsqueda por nombre. El texto viaja como PARÁMETRO (:texto), nunca pegado al SQL
     * con string templates: así se evita la inyección SQL.
     * Los % del LIKE los agrega el propio SQL con el operador || (concatenar).
     */
    @Query("SELECT * FROM farmacias WHERE nombre LIKE '%' || :texto || '%' ORDER BY posicion ASC")
    fun buscarPorNombre(texto: String): Flow<List<FarmaciaEntity>>

    /** Cantidad de filas guardadas (sirve para saber si la tabla está vacía). */
    @Query("SELECT COUNT(*) FROM farmacias")
    suspend fun contar(): Int

    @Query("DELETE FROM farmacias")
    suspend fun borrarTodos()
}
