package com.baff.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import com.baff.launcher.apps.AppInfo
import com.baff.launcher.apps.AppRepository
import com.baff.launcher.icons.IconRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var iconRenderer: IconRenderer
    private lateinit var appGrid: GridLayout
    private lateinit var apps: List<AppInfo>
    private val iconSizePx by lazy { dp(58) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconRenderer = IconRenderer(this)
        window.statusBarColor = Color.rgb(244, 245, 250)
        window.navigationBarColor = Color.rgb(244, 245, 250)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(244, 245, 250), Color.rgb(232, 237, 250))
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(20))
        }
        scroll.addView(content)
        setContentView(scroll)

        val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        content.addView(text(date.replaceFirstChar { it.uppercase() }, 14, 0xFF777D8D.toInt()))
        content.addView(text("Добро пожаловать", 29, 0xFF171923.toInt(), true).apply {
            setPadding(0, dp(5), 0, dp(22))
        })

        val section = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        section.addView(text("Избранное", 18, 0xFF202331.toInt(), true),
            LinearLayout.LayoutParams(0, dp(36), 1f))
        section.addView(text("BAFF 0.1", 11, 0xFF858B9A.toInt()))
        content.addView(section)

        appGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        content.addView(appGrid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val allAppsButton = TextView(this).apply {
            text = "Все приложения  ›"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF3D5FCB.toInt())
            background = rounded(0xFFFFFFFF.toInt(), 22)
            elevation = dp(2).toFloat()
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setOnClickListener { showAppDrawer() }
        }
        content.addView(allAppsButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(18) })

        val hint = text("Лёгкий интерфейс. Ничего лишнего.", 12, 0xFF9298A7.toInt())
        hint.gravity = Gravity.CENTER
        content.addView(hint, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        ))

        apps = AppRepository(this).getLaunchableApps()
        showFavorites()
        content.alpha = 0f
        content.animate().alpha(1f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun showFavorites() {
        appGrid.removeAllViews()
        val favorites = apps.take(8)
        favorites.forEachIndexed { index, app ->
            val tile = createAppTile(app)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(112)
                columnSpec = GridLayout.spec(index % 4, 1f)
                rowSpec = GridLayout.spec(index / 4)
                setMargins(dp(2), dp(5), dp(2), dp(5))
            }
            appGrid.addView(tile, params)
        }
    }

    private fun createAppTile(app: AppInfo): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { launchApp(app.packageName, this) }
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN ->
                        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start()
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                false
            }
        }
        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = app.label
        }
        tile.addView(icon, LinearLayout.LayoutParams(iconSizePx, iconSizePx))
        tile.addView(TextView(this).apply {
            text = app.label
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(0xFF343847.toInt())
            setPadding(0, dp(7), 0, 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        worker.execute {
            val bitmap = iconRenderer.getIcon(app.packageName, iconSizePx)
            if (bitmap != null) mainHandler.post { icon.setImageBitmap(bitmap) }
        }
        return tile
    }

    private fun showAppDrawer() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(8))
        }
        val search = EditText(this).apply {
            hint = "Поиск приложений"
            singleLine = true
            textSize = 16f
            background = rounded(0xFFF0F2F8.toInt(), 16)
            setPadding(dp(14), 0, dp(14), 0)
        }
        val list = ListView(this).apply {
            dividerHeight = dp(1)
            isFastScrollEnabled = true
        }
        val adapter = AppListAdapter(apps)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.itemAt(position)
            launchApp(selected.packageName, list)
        }
        search.addTextChangedListener(SimpleTextWatcher { query ->
            adapter.filter(query)
        })
        panel.addView(search, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        ))
        panel.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(440)
        ).apply { topMargin = dp(10) })

        AlertDialog.Builder(this)
            .setTitle("Приложения")
            .setView(panel)
            .setNegativeButton("Закрыть", null)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawable(rounded(Color.WHITE, 24))
                dialog.show()
            }
    }

    private fun launchApp(packageName: String, source: View) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        val options = ActivityOptions.makeScaleUpAnimation(
            source, 0, 0, source.width.coerceAtLeast(1), source.height.coerceAtLeast(1)
        )
        try {
            startActivity(intent, options.toBundle())
        } catch (_: Exception) {
            startActivity(intent)
        }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private inner class AppListAdapter(private val source: List<AppInfo>) : BaseAdapter() {
        private var visible = source
        override fun getCount() = visible.size
        override fun getItem(position: Int) = visible[position]
        fun itemAt(position: Int) = visible[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? TextView) ?: TextView(this@MainActivity).apply {
                textSize = 16f
                setTextColor(0xFF202331.toInt())
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
            }
            row.text = visible[position].label
            row.height = dp(52)
            return row
        }
        fun filter(query: String) {
            visible = if (query.isBlank()) source else source.filter {
                it.label.contains(query, ignoreCase = true)
            }
            notifyDataSetChanged()
        }
    }

    private class SimpleTextWatcher(private val changed: (String) -> Unit) :
        android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            changed(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
