package com.dental.ui.pricelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.dental.data.PriceListRepository
import com.dental.model.PriceListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(repository: PriceListRepository) {
    var searchQuery by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PriceListItem?>(null) }
    var addingCategory by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    fun loadItems() {
        items = repository.getAll()
    }

    LaunchedEffect(reloadKey) {
        loadItems()
    }

    var deleteMode by remember { mutableStateOf(false) }

    val filtered = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items
        else {
            val q = searchQuery.lowercase()
            items.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Прейскурант") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { deleteMode = !deleteMode }) {
                        Icon(
                            if (deleteMode) Icons.Default.DeleteSweep else Icons.Default.Delete,
                            contentDescription = if (deleteMode) "Выключить удаление" else "Режим удаления",
                            tint = if (deleteMode) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск услуг...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (category, categoryItems) ->
                    item(key = "cat_$category") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                addingCategory = category
                                showAddDialog = true
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    items(categoryItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            onClick = {
                                editingItem = item
                                showEditDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatPrice(item.defaultPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (deleteMode) {
                                    IconButton(
                                        onClick = {
                                            repository.delete(item.id)
                                            reloadKey++
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
    }

    if (showEditDialog && editingItem != null) {
        PriceItemEditDialog(
            title = "Редактировать услугу",
            initialName = editingItem!!.name,
            initialCategory = editingItem!!.category,
            initialPrice = editingItem!!.defaultPrice,
            showCategoryField = true,
            onSave = { name, category, price ->
                repository.update(editingItem!!.id, category, name, price)
                editingItem = null
                showEditDialog = false
                reloadKey++
            },
            onDismiss = {
                editingItem = null
                showEditDialog = false
            }
        )
    }

    if (showAddDialog) {
        PriceItemEditDialog(
            title = "Добавить услугу",
            initialName = "",
            initialCategory = addingCategory,
            initialPrice = 0,
            showCategoryField = true,
            onSave = { name, category, price ->
                repository.create(category, name, price)
                showAddDialog = false
                reloadKey++
            },
            onDismiss = {
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PriceItemEditDialog(
    title: String,
    initialName: String,
    initialCategory: String,
    initialPrice: Long,
    showCategoryField: Boolean,
    onSave: (name: String, category: String, price: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var priceText by remember { mutableStateOf(formatPriceInput(initialPrice)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Наименование услуги") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (showCategoryField) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Категория") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Цена (руб)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("1500") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = parsePriceInput(priceText)
                    if (name.isNotBlank() && category.isNotBlank() && price >= 0) {
                        onSave(name.trim(), category.trim(), price)
                    }
                },
                enabled = name.isNotBlank() && category.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun formatPrice(amount: Long): String {
    val rubles = amount / 100
    return rubles.toString().reversed().chunked(3).joinToString(" ").reversed()
}

private fun formatPriceInput(amount: Long): String {
    val rubles = amount / 100
    return if (rubles == 0L) "" else rubles.toString()
}

private fun parsePriceInput(text: String): Long {
    val rubles = text.filter { it.isDigit() }.toLongOrNull() ?: return -1
    return rubles * 100
}
