package com.example.notev

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_rows")
data class NoteRow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val text: String,
    val sum: String,
    val position: Int
)