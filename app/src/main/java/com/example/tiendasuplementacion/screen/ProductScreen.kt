package com.example.tiendasuplementacion.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tiendasuplementacion.component.NetworkErrorBanner
import com.example.tiendasuplementacion.viewmodel.ProductViewModel
import com.example.tiendasuplementacion.viewmodel.CartViewModel
import com.example.tiendasuplementacion.model.Product
import com.example.tiendasuplementacion.viewmodel.AuthViewModel
import com.example.tiendasuplementacion.viewmodel.CategoryProductViewModel
import com.example.tiendasuplementacion.model.CategoryProduct
import com.example.tiendasuplementacion.model.Category
import com.example.tiendasuplementacion.viewmodel.CategoryViewModel
import androidx.compose.ui.res.painterResource
import com.example.tiendasuplementacion.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    navController: NavController,
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel,
    authViewModel: AuthViewModel = viewModel(),
    categoryProductViewModel: CategoryProductViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val products by productViewModel.products.observeAsState(emptyList())
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartItemCount = cartItems.sumOf { it.quantity }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val currentUser by authViewModel.currentUser.collectAsState()
    val categoryProducts by categoryProductViewModel.relations.observeAsState(emptyList())
    val categories by categoryViewModel.categories.observeAsState(emptyList())
    val error by productViewModel.error.collectAsState()
    var showNetworkError by remember { mutableStateOf(false) }
    var networkErrorMessage by remember { mutableStateOf("") }
    val isAdmin = currentUser?.role_id == 2L

    LaunchedEffect(Unit) {
        productViewModel.fetchProducts()
        categoryProductViewModel.fetchAll()
        categoryViewModel.fetchCategories()
    }

    LaunchedEffect(error) {
        if (error != null && (error!!.contains("No se pudo conectar") || error!!.contains("599"))) {
            showNetworkError = true
            networkErrorMessage = error ?: ""
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
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
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Productos",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF6E7DF),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        onAddToCart = {
                            try {
                                cartViewModel.addToCart(it)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Error al agregar al carrito"
                                showError = true
                            }
                        },
                        categoryProducts = categoryProducts,
                        categories = categories,
                        isAdmin = isAdmin,
                        navController = navController
                    )
                }
            }
        }
        AnimatedVisibility(visible = cartItemCount > 0 && currentUser?.role_id != 2L) {
            FloatingActionButton(
                onClick = { navController.navigate("cart") },
                containerColor = Color(0xFFF6E7DF),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                BadgedBox(badge = {
                    Badge { Text(cartItemCount.toString(), color = Color(0xFF23242A)) }
                }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = Color(0xFF23242A))
                }
            }
        }
        if (currentUser?.role_id == 2L) {
            FloatingActionButton(
                onClick = { navController.navigate("productForm") },
                containerColor = Color(0xFFF6E7DF),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto", tint = Color(0xFF23242A))
            }
        }
        if (showNetworkError) {
            NetworkErrorBanner(
                message = networkErrorMessage,
                onRetry = {
                    showNetworkError = false
                    productViewModel.fetchProducts()
                },
                onDismiss = { showNetworkError = false }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onAddToCart: (Product) -> Unit,
    categoryProducts: List<CategoryProduct>,
    categories: List<Category>,
    isAdmin: Boolean = false,
    navController: NavController
) {
    val isOutOfStock = product.stock <= 0
    val categoryProduct = categoryProducts.find { it.product_id == product.id }
    val category = categoryProduct?.let { cp ->
        categories.find { it.id == cp.category_id }
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showProductDetails by remember { mutableStateOf(false) }
    val productViewModel: ProductViewModel = viewModel()

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este producto? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productViewModel.deleteProduct(product.id)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showProductDetails) {
        AlertDialog(
            onDismissRequest = { showProductDetails = false },
            title = { Text("Detalles del Producto") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    AsyncImage(
                        model = product.url_image,
                        contentDescription = product.name,
                        placeholder = painterResource(R.drawable.placeholder),
                        error = painterResource(R.drawable.image_error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ID: ${product.id}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Nombre: ${product.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Descripción: ${product.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Precio: $${product.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stock: ${product.stock}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Categoría: ${category?.name ?: "Sin categoría"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductDetails = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF26272B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = product.url_image,
                contentDescription = product.name,
                placeholder = painterResource(R.drawable.placeholder),
                error = painterResource(R.drawable.image_error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF6E7DF)
            )
            Text(
                text = category?.name ?: "Sin categoría",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF6E7DF).copy(alpha = 0.7f)
            )
            Text(
                text = "$${product.price}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF6E7DF)
            )
            Text(
                text = if (isOutOfStock) "Sin stock" else "Stock: ${product.stock}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOutOfStock) Color(0xFFD32F2F) else Color(0xFFF6E7DF).copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showProductDetails = true },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF6E7DF)
                    )
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Información del producto",
                        tint = Color(0xFF23242A)
                    )
                }
                if (isAdmin) {
                    IconButton(
                        onClick = { navController.navigate("editProduct/${product.id}") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color(0xFFF6E7DF)
                        )
                    }
                } else {
                    Button(
                        onClick = { onAddToCart(product) },
                        enabled = !isOutOfStock,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E7DF)
                        )
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Agregar al carrito",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF23242A)
                        )
                    }
                }
            }
        }
    }
}
