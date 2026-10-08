package com.example.primerpaso

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * La base de datos de la app. Es un SINGLETON: abrir una base es caro, así que toda
 * la app comparte una única instancia, que se obtiene con [getInstance].
 *
 * Si se agrega o cambia una tabla hay que subir `version`.
 */
@Database(
    entities = [FarmaciaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun farmaciaDao(): FarmaciaDao

    companion object {
        // @Volatile: los cambios de esta variable se ven enseguida desde todos los hilos.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            // Doble chequeo: si ya existe la devolvemos; si no, la creamos
            // dentro de synchronized para que dos hilos no creen dos bases a la vez.
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,   // applicationContext evita filtrar una Activity
                    AppDatabase::class.java,
                    "primerpaso.db"
                )
                    // ⚠️ SOLO PARA DESARROLLO: si cambia la versión y no hay migración,
                    // BORRA todas las tablas y las crea de nuevo (se pierden los datos).
                    // En una app publicada hay que escribir migraciones reales.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
