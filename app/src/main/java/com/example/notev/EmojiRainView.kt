package com.example.notev

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.random.Random

class EmojiRainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val emojis = listOf("🍊", "😊", "🚀", "💸", "🍕", "😎", "🔥", "✨", "👾", "💻")

    private val columnCount = 15
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 75f
        textAlign = Paint.Align.CENTER
    }

    private val bgPaint = Paint().apply {
        color = "#E6000000".toColorInt()
    }

    private class Column(
        val x: Float,
        var y: Float,
        var speed: Float,
        var length: Int
    ) {
        val chars = mutableListOf<String>()

        fun resetChars(emojiList: List<String>) {
            chars.clear()
            repeat(length) { chars.add(emojiList.random()) }
        }
    }

    private val columns = mutableListOf<Column>()
    private var isRaining = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isRaining) {
            setupColumns(w, h)
        }
    }

    private fun setupColumns(viewWidth: Int, viewHeight: Int) {
        columns.clear()
        val columnWidth = viewWidth.toFloat() / columnCount

        for (i in 0 until columnCount) {
            val x = i * columnWidth + (columnWidth / 2)
            val speed = Random.nextFloat() * 10f + 8f
            val length = Random.nextInt(4, 9)

            val startY = -Random.nextFloat() * viewHeight - 100f

            val column = Column(x, startY, speed, length)
            column.resetChars(emojis)
            columns.add(column)
        }
    }

    fun startRain() {
        isRaining = true
        visibility = VISIBLE

        post {
            if (width > 0 && height > 0) {
                if (columns.isEmpty()) {
                    setupColumns(width, height)
                }
                postInvalidateOnAnimation()
            }
        }
    }

    fun stopRain() {
        isRaining = false
        visibility = GONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isRaining) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (col in columns) {
            col.y += col.speed

            if (col.y - (col.length * paint.textSize) > height) {
                col.y = -paint.textSize
                col.speed = Random.nextFloat() * 10f + 8f
                col.length = Random.nextInt(4, 9)
                col.resetChars(emojis)
            }

            for (i in 0 until col.length) {
                val charY = col.y - (i * paint.textSize * 1.1f)

                val alpha = (255 - (i * (200 / col.length))).coerceAtLeast(40)

                paint.alpha = alpha
                canvas.drawText(col.chars[i], col.x, charY, paint)
            }
        }

        if (isRaining) {
            postInvalidateOnAnimation()
        }
    }
}