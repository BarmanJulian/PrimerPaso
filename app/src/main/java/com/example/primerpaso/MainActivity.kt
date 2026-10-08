package com.example.primerpaso

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.primerpaso.ui.theme.PrimerPasoTheme
import kotlinx.coroutines.launch

val AzulOscuro    = Color(0xFF1565C0)
val AzulMedio     = Color(0xFF42A5F5)
val AzulClaro     = Color(0xFFE3F2FD)
val AzulTexto     = Color(0xFF0D47A1)
val GrisTexto     = Color(0xFF78909C)
val GrisSeparador = Color(0xFFE3F2FD)

data class FarmaciaUI(
    val iniciales: String,
    val diaSemana: String,
    val numeroDia: String,
    val mes: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val imagen: String,
    val turnoTipo: TurnoTipo
)

enum class TurnoTipo { HOY, MANANA, PROXIMO }

data class ResultadoBusqueda(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val precio: String?,
    val imagen: String?,
    val negocio_nombre: String?,
    val telefono: String?,
    val ubicacion: String?,
    val rating: Double?
)

data class RespuestaBusqueda(
    val results: List<ResultadoBusqueda>?,
    val total: Int?,
    val message: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrimerPasoTheme {
                BuscaYaApp()
            }
        }
    }
}

/**
 * Pantalla "con estado": obtiene el ViewModel y observa el estado que sale de Room.
 * No sabe dibujar nada; delega en [BuscaYaContent].
 */
@Composable
fun BuscaYaApp() {
    val context = LocalContext.current
    val viewModel: FarmaciasViewModel = viewModel(factory = FarmaciasViewModel.Factory(context))

    // collectAsStateWithLifecycle: solo recolecta mientras la pantalla está visible.
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val actualizando by viewModel.actualizando.collectAsStateWithLifecycle()

    BuscaYaContent(
        estado       = estado,
        actualizando = actualizando,
        onActualizar = { viewModel.refrescar(forzar = true) }
    )
}

/** Pantalla "sin estado propio de datos": dibuja lo que le pasan (por eso se puede previsualizar). */
@Composable
fun BuscaYaContent(
    estado: FarmaciasUiState,
    actualizando: Boolean,
    onActualizar: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // La lista sale siempre de Room, sea cual sea el estado.
    val farmacias = when (estado) {
        FarmaciasUiState.Cargando         -> emptyList<FarmaciaEntity>()
        is FarmaciasUiState.Exito        -> estado.farmacias
        is FarmaciasUiState.SinConexion  -> estado.farmacias
        is FarmaciasUiState.Error        -> estado.farmacias
    }

    var busqueda      by remember { mutableStateOf("") }
    var resultados    by remember { mutableStateOf<List<ResultadoBusqueda>>(emptyList()) }
    var buscando      by remember { mutableStateOf(false) }
    var errorBusqueda by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulOscuro)
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BuscaYa",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "Villa del Rosario",
                fontSize = 13.sp,
                color = AzulMedio,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = {
                    Text("Buscar farmacia, negocio...", fontSize = 13.sp, color = GrisTexto)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor      = Color.Transparent,
                    unfocusedBorderColor    = Color.Transparent
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (busqueda.isNotBlank()) {
                        scope.launch {
                            buscando      = true
                            errorBusqueda = null
                            resultados    = emptyList()
                            try {
                                val resp = RetrofitClient.api.buscar(busqueda)
                                resultados = resp.results ?: emptyList()
                            } catch (e: Exception) {
                                errorBusqueda = "No se pudo realizar la búsqueda"
                                e.printStackTrace()
                            } finally {
                                buscando = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape    = RoundedCornerShape(8.dp)
            ) {
                if (buscando) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        color       = AzulOscuro,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Buscar", color = AzulOscuro, fontWeight = FontWeight.Medium)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp)
        ) {

            if (resultados.isNotEmpty()) {
                item {
                    Text(
                        text = "Resultados de búsqueda",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = AzulTexto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "${resultados.size} resultado${if (resultados.size != 1) "s" else ""} encontrado${if (resultados.size != 1) "s" else ""}",
                        fontSize = 11.sp,
                        color = GrisTexto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(top = 2.dp, bottom = 16.dp)
                    )
                }
                items(resultados) { resultado ->
                    ResultadoItem(resultado)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = AzulClaro, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (errorBusqueda != null) {
                item {
                    Text(
                        text = errorBusqueda!!,
                        fontSize = 13.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Farmacias de Turno",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = AzulTexto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                Text(
                    text = "(desde: 08:00 am hasta: 08:00 am del día siguiente)",
                    fontSize = 11.sp,
                    color = GrisTexto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(top = 2.dp, bottom = 8.dp)
                )

                // Aviso cuando no hay internet: los datos que se ven son los guardados en Room.
                if (estado is FarmaciasUiState.SinConexion) {
                    Text(
                        text = "Sin conexión, mostrando datos guardados",
                        fontSize = 12.sp,
                        color = AzulTexto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AzulClaro)
                            .padding(10.dp)
                    )
                }
                if (estado is FarmaciasUiState.Error) {
                    Text(
                        text = "No se pudo actualizar: ${estado.mensaje}",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }

                // Botón "Actualizar": fuerza una nueva descarga desde la API.
                OutlinedButton(
                    onClick  = onActualizar,
                    enabled  = !actualizando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    if (actualizando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Actualizar")
                    }
                }

                if (estado is FarmaciasUiState.Cargando) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }

            items(farmacias, key = { it.id }) { entidad ->
                val farmaciaUI = entidad.toUI()

                FarmaciaItem(farmaciaUI) {
                    val intent = Intent(context, FarmaciaDetalleActivity::class.java).apply {
                        putExtra("nombre",    farmaciaUI.nombre)
                        putExtra("direccion", farmaciaUI.direccion)
                        putExtra("telefono",  farmaciaUI.telefono)
                        putExtra("imagen",    farmaciaUI.imagen)
                        putExtra("diaSemana", farmaciaUI.diaSemana)
                        putExtra("numeroDia", farmaciaUI.numeroDia)
                        putExtra("mes",       farmaciaUI.mes)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun ResultadoItem(resultado: ResultadoBusqueda) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AzulClaro),
            contentAlignment = Alignment.Center
        ) {
            if (!resultado.imagen.isNullOrBlank()) {
                AsyncImage(
                    model              = resultado.imagen,
                    contentDescription = resultado.nombre,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                val ini = resultado.nombre.split(" ")
                    .take(2).joinToString("") { it.first().toString() }
                Text(ini.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulOscuro)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = resultado.nombre,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF1A1A1A)
            )
            if (!resultado.descripcion.isNullOrBlank()) {
                Text(text = resultado.descripcion, fontSize = 11.sp, color = GrisTexto)
            }
            if (!resultado.ubicacion.isNullOrBlank()) {
                Text(text = resultado.ubicacion, fontSize = 11.sp, color = GrisTexto)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (resultado.rating != null) {
                    Text("★", fontSize = 11.sp, color = Color(0xFFFFC107))
                    Text(" ${resultado.rating}", fontSize = 11.sp, color = GrisTexto)
                }
                if (!resultado.telefono.isNullOrBlank()) {
                    Text(" · ${resultado.telefono}", fontSize = 11.sp, color = GrisTexto)
                }
            }
        }
        Text("›", fontSize = 22.sp, color = GrisTexto)
    }
    HorizontalDivider(color = GrisSeparador, thickness = 0.5.dp)
}

@Composable
fun FarmaciaItem(farmacia: FarmaciaUI, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AzulClaro),
            contentAlignment = Alignment.Center
        ) {
            if (farmacia.imagen.isNotBlank()) {
                AsyncImage(
                    model              = farmacia.imagen,
                    contentDescription = farmacia.nombre,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text       = farmacia.iniciales,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AzulOscuro
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(48.dp)
        ) {
            Text(farmacia.diaSemana, fontSize = 9.sp,  color = GrisTexto)
            Text(farmacia.numeroDia, fontSize = 20.sp, color = AzulOscuro, fontWeight = FontWeight.Medium)
            Text(farmacia.mes,       fontSize = 9.sp,  color = GrisTexto)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = farmacia.nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text     = "${farmacia.direccion} · ${farmacia.telefono}",
                fontSize = 11.sp,
                color    = GrisTexto
            )
        }
        Text("›", fontSize = 22.sp, color = GrisTexto)
    }
    HorizontalDivider(color = GrisSeparador, thickness = 0.5.dp)
}

@Preview(showBackground = true)
@Composable
fun BuscaYaPreview() {
    PrimerPasoTheme {
        BuscaYaContent(
            estado = FarmaciasUiState.SinConexion(
                listOf(
                    FarmaciaEntity(
                        id = "2026-06-15|Farmacia Central", posicion = 0,
                        fechaRaw = "15/06/2026", fechaIso = "2026-06-15", diaSemana = "Lun",
                        nombre = "Farmacia Central", direccion = "San Martín 123",
                        imagen = "", telefono = "03573 421234", fechaActualizacion = 0L
                    )
                )
            ),
            actualizando = false,
            onActualizar = {}
        )
    }
}