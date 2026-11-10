package com.example.notev

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditNoteActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var rowAdapter: NoteRowAdapter
    private var currentNoteId: Int = 0
    private lateinit var note: Note

    private lateinit var tvDate: TextView
    private lateinit var etTitle: EditText
    private lateinit var tvTotalSum: TextView
    private lateinit var switchDiscount: SwitchMaterial

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(getColor(R.color.bg_soft))
        )
        window.isNavigationBarContrastEnforced = false
        window.navigationBarColor = getColor(R.color.bg_soft)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_edit_note)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbarEdit)
        tvDate = findViewById(R.id.tvDate)
        etTitle = findViewById(R.id.etTitle)
        tvTotalSum = findViewById(R.id.tvTotalSum)
        val fabAddRow = findViewById<ImageButton>(R.id.fabAddRow)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEdit)
        switchDiscount = findViewById(R.id.switchDiscount)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            if (ime.bottom > 0) {
                fabAddRow.visibility = View.INVISIBLE
                recyclerView.setPadding(0, 0, 0, ime.bottom)
            } else {
                fabAddRow.visibility = View.VISIBLE
                currentFocus?.clearFocus()

                val basePadding = (160 * resources.displayMetrics.density).toInt()
                recyclerView.setPadding(0, 0, 0, basePadding)

                val params = fabAddRow.layoutParams as android.view.ViewGroup.MarginLayoutParams
                val baseMargin = (32 * resources.displayMetrics.density).toInt()
                params.bottomMargin = baseMargin + systemBars.bottom
                fabAddRow.layoutParams = params
            }
            insets
        }

        currentNoteId = intent.getIntExtra("NOTE_ID", 0)

        rowAdapter = NoteRowAdapter(
            onSumChanged = { calculateTotalSum() },
            onRowUpdated = { updatedRow ->
                lifecycleScope.launch { db.noteDao().updateRow(updatedRow) }
            },
            onRowLongClick = { rowToDelete ->
                AlertDialog.Builder(this@EditNoteActivity)
                    .setTitle("Удалить строку?")
                    .setMessage("Вы уверены, что хотите удалить эту строку?")
                    .setPositiveButton("Да") { _, _ ->
                        lifecycleScope.launch {
                            db.noteDao().deleteRow(rowToDelete)
                            rowAdapter.removeRow(rowToDelete)
                            calculateTotalSum()
                        }
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = rowAdapter

        if (currentNoteId == 0) {
            val currentDate = getCurrentDate()
            tvDate.text = currentDate
            lifecycleScope.launch {
                val newNote = Note(date = currentDate, title = "")
                currentNoteId = db.noteDao().insertNote(newNote).toInt()
                loadNoteData()
            }
        } else {
            loadNoteData()
        }

        switchDiscount.setOnCheckedChangeListener { _, isChecked ->
            if (::note.isInitialized) {
                note = note.copy(isDiscountApplied = isChecked)
                lifecycleScope.launch { db.noteDao().updateNote(note) }
                calculateTotalSum()
            }
        }

        fabAddRow.setOnClickListener {
            val currentRows = rowAdapter.getRows()

            if (currentRows.isNotEmpty()) {
                val lastRow = currentRows.last()
                if (lastRow.text.trim().isEmpty() && lastRow.sum.trim().isEmpty()) {
                    Toast.makeText(this, "Сначала заполните предыдущую строку", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            lifecycleScope.launch {
                val newPosition = currentRows.size + 1
                val newRow = NoteRow(noteId = currentNoteId, text = "", sum = "", position = newPosition)
                val id = db.noteDao().insertRow(newRow)
                rowAdapter.addRow(newRow.copy(id = id.toInt()))
                recyclerView.postDelayed({
                    recyclerView.smoothScrollToPosition(rowAdapter.itemCount - 1)
                }, 200)
            }
        }

        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (::note.isInitialized) {
                    note = note.copy(title = s.toString())
                    lifecycleScope.launch { db.noteDao().updateNote(note) }
                }
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    if (::note.isInitialized) {
                        val rows = rowAdapter.getRows()
                        val title = etTitle.text.toString().trim()
                        val isEmpty = title.isEmpty() && rows.all { it.text.trim().isEmpty() }
                        if (isEmpty) {
                            db.noteDao().deleteNote(note)
                        }
                    }
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadNoteData() {
        lifecycleScope.launch {
            val loadedNote = db.noteDao().getNoteById(currentNoteId)
            if (loadedNote != null) {
                note = loadedNote
                tvDate.text = note.date
                if (etTitle.text.toString() != note.title) {
                    etTitle.setText(note.title)
                }
                // Обновляем ползунок при загрузке (без вызова слушателя)
                switchDiscount.isChecked = note.isDiscountApplied
            }
            val rows = db.noteDao().getRowsForNote(currentNoteId).first()
            rowAdapter.submitList(rows)
            calculateTotalSum()
        }
    }

    private fun calculateTotalSum() {
        var total = 0.0
        rowAdapter.getRows().forEach { row ->
            total += row.sum.toDoubleOrNull() ?: 0.0
        }

        if (::note.isInitialized && note.isDiscountApplied && total > 0) {
            total = (total - 30.0).coerceAtLeast(0.0)
        }

        tvTotalSum.text = total.toInt().toString()

        if (::note.isInitialized) {
            note = note.copy(totalSum = total)
            lifecycleScope.launch {
                db.noteDao().updateNote(note)
            }
        }
    }

    private fun getCurrentDate(): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("ru"))
        val dateString = dateFormat.format(now)
        val dayFormat = SimpleDateFormat("EE", Locale.forLanguageTag("ru"))
        var dayOfWeek = dayFormat.format(now)
        dayOfWeek = dayOfWeek.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return "$dateString.$dayOfWeek."
    }
}