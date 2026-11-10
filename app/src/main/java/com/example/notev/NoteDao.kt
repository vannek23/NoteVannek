package com.example.notev

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Query("SELECT * FROM note_rows WHERE noteId = :noteId ORDER BY position ASC")
    fun getRowsForNote(noteId: Int): Flow<List<NoteRow>>

    @Insert
    suspend fun insertRow(row: NoteRow): Long

    @Delete
    suspend fun deleteRow(row: NoteRow)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Update
    suspend fun updateNote(note: Note)

    @Update
    suspend fun updateRow(row: NoteRow)
}