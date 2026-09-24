package com.chayzay.catequesisapp.game

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.chayzay.catequesisapp.R

/** Port de las casillas y los listeners de arrastre del juego antiguo. */
internal class EnigmaBoard(context: Context, private val word: String, private val onSolved: () -> Unit) :
    LinearLayout(context) {
    private val slots = mutableListOf<LinearLayout>()
    private val sourceRow = LinearLayout(context).apply { gravity = Gravity.CENTER }
    private var completed = false
    private val cellWidth = (32 * resources.displayMetrics.density).toInt()
    private val cellHeight = (48 * resources.displayMetrics.density).toInt()
    private val margin = (5 * resources.displayMetrics.density).toInt()

    init {
        orientation = VERTICAL
        val slotRow = LinearLayout(context).apply { gravity = Gravity.CENTER }
        word.forEach {
            val slot = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.background_grid_class_cornes)
                layoutParams = cellParams()
            }
            slot.setOnDragListener { _, event ->
                if (event.action == DragEvent.ACTION_DROP && !completed) {
                    val letter = event.localState as? Button ?: return@setOnDragListener true
                    val origin = letter.parent as? ViewGroup ?: return@setOnDragListener true
                    if (origin !== slot) {
                        val swapped = slot.getChildAt(0)
                        if (swapped != null) {
                            slot.removeView(swapped)
                            origin.removeView(letter)
                            swapped.layoutParams = if (origin === sourceRow) cellParams()
                                else LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                            origin.addView(swapped)
                        } else origin.removeView(letter)
                        letter.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        slot.addView(letter)
                        checkSolved()
                    }
                }
                if (event.action == DragEvent.ACTION_DRAG_ENDED) (event.localState as? View)?.visibility = VISIBLE
                true
            }
            slots += slot
            slotRow.addView(slot)
        }
        val targetScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(slotRow)
        }
        addView(targetScroll, LayoutParams(LayoutParams.MATCH_PARENT, cellHeight + 2 * margin).apply {
            topMargin = (20 * resources.displayMetrics.density).toInt()
        })
        sourceRow.setOnDragListener { _, event ->
            if (event.action == DragEvent.ACTION_DROP && !completed) {
                val letter = event.localState as? Button ?: return@setOnDragListener true
                val owner = letter.parent as? ViewGroup ?: return@setOnDragListener true
                if (owner !== sourceRow) {
                    owner.removeView(letter)
                    letter.layoutParams = cellParams()
                    sourceRow.addView(letter)
                }
            }
            if (event.action == DragEvent.ACTION_DRAG_ENDED) (event.localState as? View)?.visibility = VISIBLE
            true
        }
        word.toList().shuffled().forEach { character ->
            val button = Button(context).apply {
                text = character.toString()
                textSize = 17f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 0, 0, 0)
                setBackgroundResource(R.drawable.button_letter_enigma)
                layoutParams = cellParams()
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && !completed) {
                        val data = ClipData.newPlainText("letter", (view as Button).text)
                        view.visibility = INVISIBLE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            view.startDragAndDrop(data, View.DragShadowBuilder(view), view, 0)
                        } else view.startDrag(data, View.DragShadowBuilder(view), view, 0)
                        true
                    } else event.action == MotionEvent.ACTION_UP
                }
            }
            sourceRow.addView(button)
        }
        val lettersScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(sourceRow)
        }
        addView(lettersScroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
            gravity = Gravity.CENTER_VERTICAL
        })
    }

    private fun cellParams() = LayoutParams(cellWidth, cellHeight).apply {
        leftMargin = margin
        rightMargin = margin
    }

    private fun checkSolved() {
        if (slots.any { it.childCount != 1 }) return
        if (slots.joinToString("") { (it.getChildAt(0) as Button).text } == word) {
            completed = true
            onSolved()
        }
    }
}
