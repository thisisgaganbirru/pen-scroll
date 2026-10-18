package com.max.penscroll

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The "Pen Scroll Smart" mapping: a row for the default action plus one row per mapped app.
 * Tapping a row's button picks its action; app rows can also be removed from that dialog.
 */
class PerAppCard(private val activity: Activity, card: LinearLayout) {

    private val rows = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    init {
        card.addView(rows)
        card.addView(
            Button(activity, null, 0, R.style.Button_Tonal).apply {
                setText(R.string.add_app)
                setOnClickListener { pickApp() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16) }
            }
        )
        render()
    }

    private fun dp(value: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), activity.resources.displayMetrics
    ).toInt()

    private fun render() {
        rows.removeAllViews()
        addRow(activity.getString(R.string.default_row), Prefs.defaultAction(activity), first = true) {
            if (it != null) Prefs.setDefaultAction(activity, it)
        }
        Prefs.mappedApps(activity).sortedBy { appLabel(it).lowercase() }.forEach { pkg ->
            val action = Prefs.appAction(activity, pkg) ?: return@forEach
            addRow(appLabel(pkg), action, first = false, removable = true) {
                if (it == null) Prefs.removeApp(activity, pkg) else Prefs.setAppAction(activity, pkg, it)
            }
        }
    }

    /** [onPicked] gets the chosen action, or null when "Remove" was chosen. */
    private fun addRow(
        title: String, current: PenAction, first: Boolean, removable: Boolean = false,
        onPicked: (PenAction?) -> Unit,
    ) {
        val label = TextView(activity).apply {
            text = title
            textSize = 15f
            setTextColor(activity.getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = dp(12) }
        }
        val button = Button(activity, null, 0, R.style.Button_Tonal).apply {
            setText(current.label)
            isAllCaps = false
            setOnClickListener { pickAction(title, removable, onPicked) }
        }
        rows.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (!first) setPadding(0, dp(10), 0, 0)
            addView(label)
            addView(button)
        })
    }

    private fun pickAction(title: String, removable: Boolean, onPicked: (PenAction?) -> Unit) {
        val actions = PenAction.entries
        val names = actions.map { activity.getString(it.label) } +
            if (removable) listOf(activity.getString(R.string.remove_app)) else emptyList()
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setItems(names.toTypedArray()) { _, which ->
                onPicked(actions.getOrNull(which))
                render()
            }
            .show()
    }

    private fun pickApp() {
        val pm = activity.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val mapped = Prefs.mappedApps(activity)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != activity.packageName && it.first !in mapped }
            .sortedBy { it.second.lowercase() }
        AlertDialog.Builder(activity)
            .setTitle(R.string.choose_app)
            .setItems(apps.map { it.second }.toTypedArray()) { _, which ->
                Prefs.setAppAction(activity, apps[which].first, Prefs.defaultAction(activity))
                render()
            }
            .show()
    }

    private fun appLabel(packageName: String): String = try {
        val pm = activity.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
}
