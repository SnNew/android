package com.example.tiendasuplementacion.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tiendasuplementacion.viewmodel.CartViewModel
import com.example.tiendasuplementacion.component.NetworkErrorBanner
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    cartViewModel: CartViewModel
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val error by cartViewModel.error.collectAsState()
    var showNetworkError by remember { mutableStateOf(false) }
    var networkErrorMessage by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(error) {
        val errorValue = error
        if (errorValue != null && (errorValue.contains("No se pudo conectar") || errorValue.contains("599"))) {
            showNetworkError = true
            networkErrorMessage = errorValue
        }
    }

    if (showDeleteConfirmation && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                productToDelete = null
            },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este producto del carrito?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToDelete?.let { cartViewModel.removeFromCart(it) }
                        showDeleteConfirmation = false
                        productToDelete = null
                    }
                ) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        productToDelete = null
                    }
                ) {
                    Text("No")
                }
            }
        )
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
                text = "Carrito de Compras",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF6E7DF),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tu carrito está vacío.", color = Color(0xFFF6E7DF).copy(alpha = 0.7f))
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    cartItems.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            elevation = CardDefaults.cardElevation(10.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF26272B)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(item.product.url_image),
                                    contentDescription = item.product.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, style = MaterialTheme.typography.titleMedium, color = Color(0xFFF6E7DF))
                                    Text("Precio: $${item.product.price * item.quantity}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF6E7DF).copy(alpha = 0.8f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1) {
                                                cartViewModel.updateQuantity(item.product.id, item.quantity - 1)
                                            }
                                        },
                                        enabled = item.quantity > 1,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = Color(0xFFF6E7DF)
                                        )
                                    ) {
                                        Text("-", color = Color(0xFFF6E7DF))
                                    }
                                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF6E7DF))
                                    IconButton(
                                        onClick = {
                                            cartViewModel.updateQuantity(item.product.id, item.quantity + 1)
                                        },
                                        enabled = item.quantity < item.product.stock,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = Color(0xFFF6E7DF)
                                        )
                                    ) {
                                        Text("+", color = Color(0xFFF6E7DF))
                                    }
                                }
                                IconButton(
                                    onClick = { 
                                        productToDelete = item.product.id
                                        showDeleteConfirmation = true
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (cartItems.isNotEmpty()) {
                Text(
                    "Total: $${String.format("%.2f", cartViewModel.getTotalPrice())}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF23242A), // Color más oscuro para el total
                    modifier = Modifier.align(Alignment.End)
                )
                val totalProductos = cartItems.sumOf { it.quantity }
                Text(
                    "Total de productos: $totalProductos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF23242A), // Color más oscuro para el total de productos
                    modifier = Modifier.align(Alignment.End)
                )
            }
            if (showNetworkError) {
                NetworkErrorBanner(
                    message = networkErrorMessage,
                    onRetry = {
                        showNetworkError = false
                        // Si tienes un método para recargar el carrito, llámalo aquí
                        // cartViewModel.fetchCart()
                    },
                    onDismiss = { showNetworkError = false }
                )
            }
        }

        // Botones fijos en la parte inferior
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color(0xFF26272B), // Mismo color que las tarjetas del carrito
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Totales arriba de los botones
                    Text(
                        "Total: $${String.format("%.2f", cartViewModel.getTotalPrice())}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF6E7DF), // Color claro para el total
                        modifier = Modifier.align(Alignment.End)
                    )
                    val totalProductos = cartItems.sumOf { it.quantity }
                    Text(
                        "Total de productos: $totalProductos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF6E7DF), // Color claro para el total de productos
                        modifier = Modifier.align(Alignment.End)
                    )
                    Button(
                        onClick = {
                            navController.navigate("paymentSelection") { launchSingleTop = true }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar Orden", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("products") { launchSingleTop = true } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Seguir Comprando")
                    }
                }
            }
        }
    }
}
