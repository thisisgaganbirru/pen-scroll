package com.max.penscroll

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Small view helpers for SetupActivity, kept separate so the screen can be rendered in previews. */

/** Colors a status pill: text in [color], background a faint wash of the same color. */
fun TextView.setPill(textRes: Int, colorRes: Int) {
    val color = context.getColor(colorRes)
    setText(textRes)
    setTextColor(color)
    backgroundTintList = ColorStateList.valueOf((color and 0x00FFFFFF) or 0x2E000000)
}

/** Appends one numbered row per item in R.array.setup_steps to [card]. */
fun addSetupSteps(card: LinearLayout) {
    val context = card.context
    fun dp(value: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics
    ).toInt()

    context.resources.getStringArray(R.array.setup_steps).forEachIndexed { index, step ->
        val number = TextView(context).apply {
            text = String.format(Locale.getDefault(), "%d", index + 1)
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(context.getColor(R.color.accent))
            setBackgroundResource(R.drawable.bg_step_number)
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(14) }
        }
        val text = TextView(context).apply {
            this.text = step
            textSize = 14f
            setTextColor(context.getColor(R.color.text_secondary))
            setLineSpacing(0f, 1.2f)
            setTextIsSelectable(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            if (index > 0) setPadding(0, dp(14), 0, 0)
            addView(number)
            addView(text)
        })
    }
}
