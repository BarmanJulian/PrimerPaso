package com.example.primerpaso

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Los 4 estados posibles de la pantalla. Al ser una clase sellada, el `when`
 * de la UI está obligado a contemplar todos.
 * La lista viaja en los estados para que, pase lo que pase, se sigan viendo los datos guardados.
 */
sealed interface FarmaciasUiState {
    /** Primera carga: todavía no hay nada guardado. */
    data object Cargando : FarmaciasUiState
    /** Hay datos para mostrar. */
    data class Exito(val farmacias: List<FarmaciaEntity>) : FarmaciasUiState
    /** No hay internet: se muestran los datos guardados con un aviso. */
    data class SinConexion(val farmacias: List<FarmaciaEntity>) : FarmaciasUiState
    /** La API falló. También se conservan los datos guardados, si los hay. */
    data class Error(
        val mensaje: String,
        val farmacias: List<FarmaciaEntity> = emptyList()
    ) : FarmaciasUiState
}

/** Cómo salió el último intento de hablar con la API (interno del ViewModel). */
private sealed interface EstadoRed {
    data object Cargando : EstadoRed
    data object Listo : EstadoRed
    data object SinConexion : EstadoRed
    data class Fallo(val mensaje: String) : EstadoRed
}

@OptIn(ExperimentalCoroutinesApi::class)
class FarmaciasViewModel(private val repositorio: FarmaciaRepository) : ViewModel() {

    private val estadoRed = MutableStateFlow<EstadoRed>(EstadoRed.Cargando)

    /** Texto que escribió el usuario en el buscador. */
    private val textoBusqueda = MutableStateFlow("")

    /**
     * Lista que sale de Room ya filtrada. flatMapLatest: cada vez que cambia el texto
     * se cancela la consulta anterior y se abre otra query LIKE en la base.
     */
    private val farmaciasFiltradas = textoBusqueda.flatMapLatest { texto ->
        repositorio.buscar(texto)
    }

    private val _actualizando = MutableStateFlow(false)
    /** true mientras hay una llamada a la API en curso (para deshabilitar el botón). */
    val actualizando: StateFlow<Boolean> = _actualizando.asStateFlow()

    /**
     * Combina las dos fuentes: la lista que sale de Room y el resultado de la última llamada a la API.
     * La lista SIEMPRE viene de Room; la API solo decide qué aviso mostrar.
     */
    val uiState: StateFlow<FarmaciasUiState> = combine(
        farmaciasFiltradas,
        estadoRed
    ) { lista, red ->
        when (red) {
            EstadoRed.Cargando    -> if (lista.isEmpty()) FarmaciasUiState.Cargando
                                     else FarmaciasUiState.Exito(lista)
            EstadoRed.Listo       -> FarmaciasUiState.Exito(lista)
            EstadoRed.SinConexion -> FarmaciasUiState.SinConexion(lista)
            is EstadoRed.Fallo    -> FarmaciasUiState.Error(red.mensaje, lista)
        }
    }.stateIn(
        scope = viewModelScope,
        // Deja de observar 5 s después de que la pantalla desaparece (por ejemplo, al rotar no se reinicia).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FarmaciasUiState.Cargando
    )

    init {
        refrescar()   // Al iniciar: solo llama a la API si Room está vacío.
    }

    /** Se llama en cada letra que escribe el usuario: la lista se actualiza en tiempo real. */
    fun filtrar(texto: String) {
        textoBusqueda.value = texto
    }

    /** @param forzar true = botón "Actualizar": siempre intenta bajar datos nuevos. */
    fun refrescar(forzar: Boolean = false) {
        if (_actualizando.value) return   // evita llamadas duplicadas si tocan el botón varias veces
        viewModelScope.launch {
            _actualizando.value = true
            estadoRed.value = EstadoRed.Cargando
            estadoRed.value = when (val resultado = repositorio.refrescar(forzar)) {
                ResultadoRefresco.Ok          -> EstadoRed.Listo
                ResultadoRefresco.SinConexion -> EstadoRed.SinConexion
                is ResultadoRefresco.Fallo    -> EstadoRed.Fallo(resultado.mensaje)
            }
            _actualizando.value = false
        }
    }

    /**
     * Fábrica simple (sin Hilt): le dice a Android cómo construir el ViewModel,
     * porque necesita un repositorio como parámetro.
     */
    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repositorio = FarmaciaRepository(
                db  = AppDatabase.getInstance(appContext),
                api = RetrofitClient.api
            )
            return FarmaciasViewModel(repositorio) as T
        }
    }
}
