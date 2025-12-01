package com.example.roomdemo


import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomdemo.ui.theme.RoomDemoTheme

// --- Composable компоненты ---
@Composable
fun TitleRow(head1: String, head2: String, head3: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Text(head1, color = Color.White, modifier = Modifier.weight(0.1f))
        Text(head2, color = Color.White, modifier = Modifier.weight(0.2f))
        Text(head3, color = Color.White, modifier = Modifier.weight(0.2f))
    }
}

@Composable
fun ProductRow(id: Int, name: String, quantity: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Text(id.toString(), modifier = Modifier.weight(0.1f))
        Text(name, modifier = Modifier.weight(0.2f))
        Text(quantity.toString(), modifier = Modifier.weight(0.2f))
    }
}

@Composable
fun CustomTextField(
    title: String,
    textState: String,
    onTextChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = textState,
        onValueChange = { onTextChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        label = { Text(title) },
        modifier = Modifier.padding(10.dp),
        textStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp)
    )
}

// --- ViewModel Factory ---
class MainViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(application) as T
    }
}

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoomDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val owner = LocalViewModelStoreOwner.current
                    owner?.let {
                        val viewModel: MainViewModel = viewModel(
                            it,
                            "MainViewModel",
                            MainViewModelFactory(LocalContext.current.applicationContext as Application)
                        )
                        ScreenSetup(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

// --- Настройка экрана с ViewModel ---
@Composable
fun ScreenSetup(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val allProducts by viewModel.allProducts.observeAsState(listOf())
    val searchResults by viewModel.searchResults.observeAsState(listOf())
    MainScreen(
        modifier = modifier,
        allProducts = allProducts,
        searchResults = searchResults,
        viewModel = viewModel
    )
}

// --- Главный UI ---
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    allProducts: List<Product>,
    searchResults: List<Product>,
    viewModel: MainViewModel
) {
    var productName by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Поля ввода
        CustomTextField(
            title = "enter the product",
            textState = productName,
            onTextChange = { productName = it },
            keyboardType = KeyboardType.Text
        )
        CustomTextField(
            title = "enter the quantity",
            textState = productQuantity,
            onTextChange = { productQuantity = it },
            keyboardType = KeyboardType.Number
        )

        // Показ ошибок
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Кнопки
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Button(onClick = {
                errorMessage = null
                if (productName.isBlank()) {
                    errorMessage = "Product name cannot be empty"
                    return@Button
                }

                val quantity = try {
                    productQuantity.toInt()
                } catch (e: NumberFormatException) {
                    errorMessage = "Please enter a valid quantity"
                    return@Button
                }

                if (quantity <= 0) {
                    errorMessage = "Quantity must be greater than 0"
                    return@Button
                }

                viewModel.insertProduct(Product(productName, quantity))
                searching = false

                // Очистка полей после успешного добавления
                productName = ""
                productQuantity = ""

            }) { Text("Add") }

            Button(onClick = {
                errorMessage = null
                if (productName.isBlank()) {
                    errorMessage = "Please enter product name to search"
                    return@Button
                }
                searching = true
                viewModel.findProduct(productName)
            }) { Text("Search") }

            Button(onClick = {
                errorMessage = null
                if (productName.isBlank()) {
                    errorMessage = "Please enter product name to delete"
                    return@Button
                }
                searching = false
                viewModel.deleteProduct(productName)
                productName = ""
                productQuantity = ""
            }) { Text("Delete") }

            Button(onClick = {
                errorMessage = null
                searching = false
                productName = ""
                productQuantity = ""
            }) { Text("Clear") }
        }


        // Список продуктов
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // ← ЭТА СТРОЧКА ВАЖНА
                .padding(10.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                val list = if (searching) searchResults else allProducts

                item {
                    TitleRow(head1 = "ID", head2 = "Product", head3 = "Quantity")
                }

                items(list) { product ->
                    ProductRow(
                        id = product.id,
                        name = product.productName,
                        quantity = product.quantity
                    )
                }
            }
        }
    }
}