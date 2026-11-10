package com.example.notev

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: NoteAdapter

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

        setContentView(R.layout.activity_main)

        val fabAddNote = findViewById<ImageButton>(R.id.fabAddNote)
        fabAddNote.addPressScaleAnimation()
        val emptyStateLayout = findViewById<LinearLayout>(R.id.emptyStateLayout)

        // --- ПАСХАЛКА ---
        val emojiRainView = findViewById<EmojiRainView>(R.id.emojiRainView)
        val titleContainer = findViewById<android.widget.FrameLayout>(R.id.titleContainer)
        var clickCount = 0
        var lastClickTime: Long = 0

        titleContainer.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            // Если прошло больше 1.5 секунд с последнего клика, счетчик сбрасывается
            if (currentTime - lastClickTime > 1500) {
                clickCount = 0
            }

            clickCount++
            lastClickTime = currentTime

            if (clickCount == 5) {
                // Запускаем дождь из смайликов
                emojiRainView.startRain()

                // Останавливаем через 7 секунд
                android.os.Handler(mainLooper).postDelayed({
                    emojiRainView.stopRain()
                }, 15000)

                clickCount = 0 // Сбрасываем счетчик
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = fabAddNote.layoutParams as android.view.ViewGroup.MarginLayoutParams
            val baseMargin = (32 * resources.displayMetrics.density).toInt()
            params.bottomMargin = baseMargin + systemBars.bottom
            fabAddNote.layoutParams = params
            insets
        }

        db = AppDatabase.getDatabase(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewNotes)
        adapter = NoteAdapter(
            onClick = { note ->
                val intent = Intent(this, EditNoteActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)
                startActivity(intent)
            },
            onLongClick = { note ->
                AlertDialog.Builder(this)
                    .setTitle("Удалить заметку?")
                    .setMessage("Вы уверены, что хотите удалить \"${note.title}\"?")
                    .setPositiveButton("Да") { _, _ ->
                        lifecycleScope.launch {
                            db.noteDao().deleteNote(note)
                            Toast.makeText(this@MainActivity, "Заметка удалена", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.noteDao().getAllNotes().collect { notes ->
                adapter.submitList(notes)
                emptyStateLayout.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fabAddNote.setOnClickListener {
            val intent = Intent(this, EditNoteActivity::class.java)
            startActivity(intent)
        }
    }
}