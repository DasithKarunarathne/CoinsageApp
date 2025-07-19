package com.example.coinsage.data.model

import java.math.BigDecimal
import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: BigDecimal,
    val category: Category,
    val date: Date,
    val type: TransactionType,
    val notes: String? = null
)

enum class Category {
    FOOD,
    TRANSPORT,
    BILLS,
    ENTERTAINMENT,
    INCOME,
    OTHER
}

enum class TransactionType {
    INCOME,
    EXPENSE
} 