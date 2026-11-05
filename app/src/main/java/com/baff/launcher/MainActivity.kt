package com.baff.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var iconRenderer: IconRenderer
    private lateinit var apps: List<AppInfo>
    private lateinit var appGrid: GridLayout
    private var defaultPromptShown = false
    private val iconSizePx by lazy { dp(58) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconRenderer = IconRenderer(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        apps = AppRepository(this).getLaunchableApps()
        buildHomeScreen()
    }

    override fun onResume() {
        super.onResume()
        if (!defaultPromptShown && !isDefaultLauncher()) {
            defaultPromptShown = true
            mainHandler.post { showDefaultLauncherPrompt() }
        }
    }

    private fun buildHomeScreen() {
        val root = FrameLayout(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, dp(42), 0, dp(126))
            setBackgroundColor(Color.TRANSPARENT)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(12), dp(22), dp(12))
        }
        val date = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
            .format(Date()).replaceFirstChar { it.uppercase() }
        content.addView(TextView(this).apply {
            text = date
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }, LinearLayout.LayoutParams(-1, dp(30)))
        content.addView(TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 76f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(12f, 0f, 3f, Color.BLACK)
        }, LinearLayout.LayoutParams(-1, dp(112)))

        val heading = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(TextView(this).apply {
            text = "Приложения"
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        heading.addView(TextView(this).apply {
            text = "⚙"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.argb(115, 20, 20, 25), 16)
            setOnClickListener { showLauncherSettings() }
        }, LinearLayout.LayoutParams(dp(46), dp(42)))
        content.addView(heading)

        appGrid = GridLayout(this).apply { columnCount = 4 }
        content.addView(appGrid, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.argb(190, 15, 16, 22), 28).apply {
                setStroke(dp(1), Color.argb(55, 255, 255, 255))
            }
            elevation = dp(12).toFloat()
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        apps.take(4).forEach { app ->
            dock.addView(createDockIcon(app), LinearLayout.LayoutParams(0, dp(68), 1f))
        }
        root.addView(dock, FrameLayout.LayoutParams(-1, dp(88), Gravity.BOTTOM).apply {
            leftMargin = dp(22)
            rightMargin = dp(22)
            bottomMargin = dp(24)
        })
        setContentView(root)
        showHomeApps()
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(240).start()
    }

    private fun showHomeApps() {
        appGrid.removeAllViews()
        apps.drop(4).take(12).forEachIndexed { index, app ->
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(108)
                columnSpec = GridLayout.spec(index % 4, 1f)
                rowSpec = GridLayout.spec(index / 4)
                setMargins(dp(2), dp(5), dp(2), dp(5))
            }
            appGrid.addView(createAppTile(app), params)
        }
    }

    private fun createAppTile(app: AppInfo): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { launchApp(app.packageName, this) }
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
            setTextColor(Color.WHITE)
            setShadowLayer(5f, 0f, 1f, Color.BLACK)
            setPadding(0, dp(6), 0, 0)
        }, LinearLayout.LayoutParams(-1, dp(27)))
        loadIcon(app, icon)
        return tile
    }

    private fun createDockIcon(app: AppInfo): View {
        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = app.label
            setPadding(dp(5), dp(4), dp(5), dp(4))
            setOnClickListener { launchApp(app.packageName, this) }
        }
        loadIcon(app, icon)
        return icon
    }

    private fun loadIcon(app: AppInfo, target: ImageView) {
        worker.execute {
            val bitmap = iconRenderer.getIcon(app.packageName, iconSizePx)
            if (bitmap != null) mainHandler.post { target.setImageBitmap(bitmap) }
        }
    }

    private fun showAppDrawer() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }
        val search = EditText(this).apply {
            hint = "Поиск приложений"
            setSingleLine(true)
            textSize = 16f
            background = rounded(0xFFF0F1F5.toInt(), 16)
            setPadding(dp(14), 0, dp(14), 0)
        }
        val list = ListView(this)
        val adapter = AppListAdapter(apps)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            launchApp(adapter.itemAt(position).packageName, list)
        }
        search.addTextChangedListener(SimpleTextWatcher { adapter.filter(it) })
        panel.addView(search, LinearLayout.LayoutParams(-1, dp(50)))
        panel.addView(list, LinearLayout.LayoutParams(-1, dp(430)).apply {
            topMargin = dp(10)
        })
        AlertDialog.Builder(this).setTitle("Все приложения").setView(panel)
            .setNegativeButton("Закрыть", null).show()
    }

    private fun showLauncherSettings() {
        val options = arrayOf("Выбрать BAFF по умолчанию", "Все приложения", "О лаунчере")
        AlertDialog.Builder(this).setTitle("Настройки BAFF").setItems(options) { _, which ->
            when (which) {
                0 -> openHomeSettings()
                1 -> showAppDrawer()
                2 -> AlertDialog.Builder(this).setTitle("BAFF Launcher")
                    .setMessage("Лёгкий домашний экран без рекламы. Версия 0.2.0.")
                    .setPositiveButton("ОК", null).show()
            }
        }.show()
    }

    private fun showDefaultLauncherPrompt() {
        if (isFinishing || isDefaultLauncher()) return
        AlertDialog.Builder(this)
            .setTitle("Сделать BAFF Launcher главным?")
            .setMessage("Выбери BAFF Launcher в системном списке приложений главного экрана. Текущие обои останутся прежними.")
            .setPositiveButton("Выбрать") { _, _ -> openHomeSettings() }
            .setNegativeButton("Позже", null).show()
    }

    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(home, 0)?.activityInfo ?: return false
        return resolved.packageName == packageName
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun launchApp(packageName: String, source: View) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        try {
            val options = ActivityOptions.makeScaleUpAnimation(
                source, 0, 0, source.width.coerceAtLeast(1), source.height.coerceAtLeast(1)
            )
            startActivity(intent, options.toBundle())
        } catch (_: Exception) {
            startActivity(intent)
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) =
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
            row.minimumHeight = dp(52)
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
