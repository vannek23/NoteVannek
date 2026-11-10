package com.example.notev

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val title: String,
    val totalSum: Double = 0.0,
    val isDiscountApplied: Boolean = false
)