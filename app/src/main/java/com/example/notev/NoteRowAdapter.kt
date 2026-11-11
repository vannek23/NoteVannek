package com.example.notev

import android.annotation.SuppressLint
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NoteRowAdapter(
    private val onSumChanged: () -> Unit,
    private val onRowUpdated: (NoteRow) -> Unit,
    private val onRowLongClick: (NoteRow) -> Unit
) : RecyclerView.Adapter<NoteRowAdapter.RowViewHolder>() {

    private var rows: MutableList<NoteRow> = mutableListOf()
    private var isCheckModeEnabled = false

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newRows: List<NoteRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCheckMode(enabled: Boolean) {
        android.util.Log.d("CHECK_DEBUG", "2. Адаптер получил команду: $enabled")
        isCheckModeEnabled = enabled
        notifyDataSetChanged()
    }

    fun addRow(row: NoteRow) {
        rows.add(row)
        notifyItemInserted(rows.size - 1)
    }

    fun removeRow(row: NoteRow) {
        val index = rows.indexOf(row)
        if (index != -1) {
            rows.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, rows.size)
        }
    }

    fun getRows(): List<NoteRow> = rows

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_note_row, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(rows[position], position)
    }

    override fun getItemCount() = rows.size

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvRowNumber)
        private val etText: EditText = itemView.findViewById(R.id.etRowText)
        private val etSum: EditText = itemView.findViewById(R.id.etRowSum)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvRowEmoji)

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val row = rows.getOrNull(pos) ?: return
                    val newText = s.toString()
                    if (row.text != newText) {
                        val updatedRow = row.copy(text = newText)
                        rows[pos] = updatedRow
                        onRowUpdated(updatedRow)
                    }
                }
            }
        }

        private val sumWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val row = rows.getOrNull(pos) ?: return
                    val newSum = s.toString()
                    if (row.sum != newSum) {
                        val updatedRow = row.copy(sum = newSum)
                        rows[pos] = updatedRow
                        onRowUpdated(updatedRow)
                        onSumChanged()
                    }
                }
            }
        }

        init {
            etText.addTextChangedListener(textWatcher)
            etSum.addTextChangedListener(sumWatcher)

            etText.isLongClickable = false
            etSum.isLongClickable = false

            etSum.setOnEditorActionListener { view, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    view.clearFocus()
                    val imm =
                        view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    true
                } else {
                    false
                }
            }

            tvEmoji.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val row = rows.getOrNull(pos) ?: return@setOnClickListener
                    val updatedRow = row.copy(isChecked = !row.isChecked)
                    rows[pos] = updatedRow
                    onRowUpdated(updatedRow)
                    notifyItemChanged(pos)
                }
            }

            itemView.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val row = rows.getOrNull(pos)
                    if (row != null) {
                        onRowLongClick(row)
                    }
                }
                true
            }
        }

        fun bind(row: NoteRow, position: Int) {
            android.util.Log.d(
                "CHECK_DEBUG",
                "3. Перерисовка строки $position. Режим сверки: $isCheckModeEnabled"
            )
            tvNumber.text = itemView.context.getString(R.string.row_number_format, position + 1)

            val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
            val defaultMargin = (8 * itemView.context.resources.displayMetrics.density).toInt()
            val extraMargin = (32 * itemView.context.resources.displayMetrics.density).toInt()

            if ((position + 1) % 5 == 0) {
                params.bottomMargin = defaultMargin + extraMargin
            } else {
                params.bottomMargin = defaultMargin
            }
            itemView.layoutParams = params

            etText.removeTextChangedListener(textWatcher)
            etSum.removeTextChangedListener(sumWatcher)

            if (etText.text.toString() != row.text) {
                etText.setText(row.text)
            }
            if (etSum.text.toString() != row.sum) {
                etSum.setText(row.sum)
            }

            if (isCheckModeEnabled) {
                etSum.visibility = View.GONE
                tvEmoji.visibility = View.VISIBLE

                if (row.isChecked) {
                    tvEmoji.text = "😊"
                    etText.paintFlags = etText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    etText.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.text_secondary
                        )
                    )
                } else {
                    tvEmoji.text = "⚪"
                    etText.paintFlags = etText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    etText.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.text_primary
                        )
                    )
                }

                etText.isEnabled = false
            } else {
                etSum.visibility = View.VISIBLE
                tvEmoji.visibility = View.GONE

                etText.paintFlags = etText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                etText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                etText.isEnabled = true
            }

            etText.addTextChangedListener(textWatcher)
            etSum.addTextChangedListener(sumWatcher)
        }
    }
}