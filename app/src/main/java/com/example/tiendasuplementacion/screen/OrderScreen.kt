package com.example.tiendasuplementacion.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tiendasuplementacion.viewmodel.UserDetailViewModel
import com.example.tiendasuplementacion.viewmodel.AuthViewModel
import com.example.tiendasuplementacion.component.NetworkErrorBanner
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import com.example.tiendasuplementacion.model.UserOrder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.example.tiendasuplementacion.R
import com.example.tiendasuplementacion.util.QRCodeGenerator
import com.google.gson.Gson
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import com.example.tiendasuplementacion.util.PdfGenerator
import java.io.File
import android.content.Intent
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    navController: NavController,
    userDetailViewModel: UserDetailViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    context: Context
) {
    val userDetail by userDetailViewModel.userDetail.observeAsState()
    val isLoading by userDetailViewModel.isLoading.observeAsState(false)
    val error by userDetailViewModel.error.observeAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showNetworkError by remember { mutableStateOf(false) }
    var networkErrorMessage by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<UserOrder?>(null) }
    var showOrderDetails by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            userDetailViewModel.fetchUserDetails(userId)
        }
    }

    LaunchedEffect(error) {
        if (error != null && (error!!.contains("No se pudo conectar") || error!!.contains("599"))) {
            showNetworkError = true
            networkErrorMessage = error ?: ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF23242A), // Fondo oscuro
                        Color(0xFF23242A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Historial de Compras",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF6E7DF),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFF6E7DF)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    userDetail?.orders?.let { orders ->
                        if (orders.isEmpty()) {
                            item {
                                Text(
                                    text = "No tienes pedidos aún",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFF6E7DF).copy(alpha = 0.7f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(orders) { order ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(10.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF26272B)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Pedido #${order.order_id}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF6E7DF)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Fecha: ${order.date_order}", color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                        Text("Estado: ${order.status.name}", color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                        Text("Total: $${order.total}", color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                        Text("Productos: ${order.total_products}", color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Productos:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF6E7DF)
                                        )
                                        order.products.forEach { product ->
                                            Text("• ${product.name} - $${product.price}", color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                selectedOrder = order
                                                showOrderDetails = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Ver Detalles Completos")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNetworkError) {
                NetworkErrorBanner(
                    message = networkErrorMessage,
                    onRetry = {
                        showNetworkError = false
                        currentUser?.id?.let { userId ->
                            userDetailViewModel.fetchUserDetails(userId)
                        }
                    },
                    onDismiss = { showNetworkError = false }
                )
            }
        }
    }

    if (showOrderDetails && selectedOrder != null) {
        // Generate PDF when showing order details
        val pdfPath = remember(selectedOrder) {
            selectedOrder?.let { order ->
                PdfGenerator.generateInvoicePdf(context, order)
            } ?: ""
        }

        val pdfUri = remember(pdfPath) {
            if (pdfPath.isNotEmpty()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(pdfPath)
                )
            } else null
        }

        AlertDialog(
            onDismissRequest = { 
                showOrderDetails = false
                selectedOrder = null
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Detalles del Pedido #${selectedOrder?.order_id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    // Información General
                    Text(
                        text = "Información General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F51B5)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fecha: ${selectedOrder?.date_order}", color = Color.Black)
                    Text("Estado: ${selectedOrder?.status?.name}", color = Color.Black)
                    Text("Total: $${selectedOrder?.total}", color = Color.Black)
                    Text("Cantidad de productos: ${selectedOrder?.total_products}", color = Color.Black)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información de Pago
                    Text(
                        text = "Información de Pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F51B5)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedOrder?.additionalInfoPayment?.let { paymentInfo ->
                        Text("Dirección de facturación:", color = Color.Black)
                        Text(paymentInfo.addressLine1 ?: "", color = Color.Black)
                        Text("${paymentInfo.city ?: ""}, ${paymentInfo.stateOrProvince ?: ""}", color = Color.Black)
                        Text("${paymentInfo.country ?: ""} ${paymentInfo.postalCode ?: ""}", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Productos
                    Text(
                        text = "Productos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F51B5)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedOrder?.products?.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = product.url_image,
                                contentDescription = product.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                Text(
                                    text = "$${product.price}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sección de Factura
                    if (pdfUri != null) {
                        Text(
                            text = "Factura Digital",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3F51B5)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(pdfUri, "application/pdf")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "No se encontró una aplicación para abrir PDFs",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3F51B5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Factura PDF")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOrderDetails = false
                        selectedOrder = null
                    }
                ) {
                    Text("Cerrar", color = Color(0xFF3F51B5))
                }
            }
        )
    }
}
