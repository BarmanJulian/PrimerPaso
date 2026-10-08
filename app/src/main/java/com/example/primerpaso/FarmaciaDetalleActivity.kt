package com.example.primerpaso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.primerpaso.ui.theme.PrimerPasoTheme

class FarmaciaDetalleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recibir datos del Intent
        val nombre    = intent.getStringExtra("nombre")    ?: ""
        val direccion = intent.getStringExtra("direccion") ?: ""
        val telefono  = intent.getStringExtra("telefono")  ?: ""
        val imagen    = intent.getStringExtra("imagen")    ?: ""
        val diaSemana = intent.getStringExtra("diaSemana") ?: ""
        val numeroDia = intent.getStringExtra("numeroDia") ?: ""
        val mes       = intent.getStringExtra("mes")       ?: ""

        setContent {
            PrimerPasoTheme {
                FarmaciaDetalleScreen(
                    nombre    = nombre,
                    direccion = direccion,
                    telefono  = telefono,
                    imagen    = imagen,
                    diaSemana = diaSemana,
                    numeroDia = numeroDia,
                    mes       = mes,
                    onBack    = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmaciaDetalleScreen(
    nombre: String,
    direccion: String,
    telefono: String,
    imagen: String,
    diaSemana: String,
    numeroDia: String,
    mes: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header azul (igual al de MainActivity) ──────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulOscuro)
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fila: flecha atrás + título centrado
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Text(
                    text = "BuscaYa",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text(
                text = "Villa del Rosario",
                fontSize = 13.sp,
                color = AzulMedio,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // ── Cuerpo blanco con bordes redondeados arriba ──────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp)
        ) {

            // Imagen + iniciales
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AzulClaro)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (imagen.isNotBlank()) {
                    AsyncImage(
                        model = imagen,
                        contentDescription = nombre,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val partes = nombre.split(" ")
                    val iniciales = if (partes.size >= 2)
                        "${partes[0].first()}${partes[1].first()}"
                    else nombre.take(2)
                    Text(
                        text = iniciales.uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulOscuro
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre
            Text(
                text = nombre,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AzulTexto,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = GrisSeparador)
            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de turno
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AzulClaro)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(diaSemana, fontSize = 11.sp, color = GrisTexto)
                        Text(
                            numeroDia,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulOscuro
                        )
                        Text(mes, fontSize = 11.sp, color = GrisTexto)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Día de turno",
                            fontSize = 12.sp,
                            color = GrisTexto
                        )
                        Text(
                            "$diaSemana $numeroDia de $mes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AzulTexto
                        )
                        Text(
                            "08:00 am — 08:00 am (día siguiente)",
                            fontSize = 11.sp,
                            color = GrisTexto
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de dirección
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AzulClaro)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dirección", fontSize = 12.sp, color = GrisTexto)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        direccion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = AzulTexto
                    )
                }
            }

            // Tarjeta de teléfono (solo si hay datos)
            if (telefono.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AzulClaro)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Teléfono", fontSize = 12.sp, color = GrisTexto)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            telefono,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AzulTexto
                        )
                    }
                }
            }
        }
    }
}

