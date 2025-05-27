package com.example.tiendasuplementacion.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tiendasuplementacion.viewmodel.OrderDetailViewModel
import com.example.tiendasuplementacion.util.QRCodeGenerator
import com.google.gson.Gson

@Composable
fun OrderDetailScreen(navController: NavController, viewModel: OrderDetailViewModel = viewModel()) {
    val details by viewModel.details.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.fetchOrderDetails()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Detalle de la Orden",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        details.forEach { detail ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text("Producto: ${detail.productName}", fontWeight = FontWeight.Bold)
                    Text("Cantidad: ${detail.quantity}")
                    Text("Precio: $${detail.price}")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Generate QR code with order details
                    val orderJson = remember(detail) {
                        Gson().toJson(detail)
                    }
                    
                    val qrBitmap = remember(orderJson) {
                        QRCodeGenerator.generateQRCode(orderJson)
                    }
                    
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code de la orden",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

