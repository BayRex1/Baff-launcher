package com.baff.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ActivityOptions
import android.app.Dialog
import android.view.Window
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.MotionEvent
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
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
import kotlin.math.abs
import kotlin.math.ceil

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("baff_launcher", MODE_PRIVATE) }
    private lateinit var iconRenderer: IconRenderer
    private lateinit var apps: List<AppInfo>
    private lateinit var root: FrameLayout
    private lateinit var pager: HorizontalScrollView
    private lateinit var pages: LinearLayout
    private lateinit var pageDots: TextView
    private lateinit var dock: LinearLayout
    private var drawerDialog: Dialog? = null
    private var defaultPromptShown = false
    private var pendingIconPackage: String? = null
    private var downX = 0f
    private var downY = 0f
    private val iconSizePx by lazy { dp(58) }
    private val homePackages: MutableList<String> by lazy {
        val saved = prefs.getString("home_apps", null)
        (saved?.split("|")?.filter { it.isNotBlank() }
            ?: apps.take(12).map { it.packageName }).distinct().toMutableList()
    }

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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (drawerDialog?.isShowing == true) return super.dispatchTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_DOWN) {
            downX = ev.x
            downY = ev.y
        } else if (ev.action == MotionEvent.ACTION_UP) {
            val dx = ev.x - downX
            val dy = ev.y - downY
            if (dy < -dp(90) && abs(dy) > abs(dx) * 1.25f) {
                showAppDrawer()
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        if (!defaultPromptShown && !isDefaultLauncher()) {
            defaultPromptShown = true
            mainHandler.post { showDefaultLauncherPrompt() }
        }
    }

    private fun buildHomeScreen() {
        root = FrameLayout(this)
        val vertical = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(42), 0, dp(126))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), 0)
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
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(54)))
        pager = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        pages = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pager.addView(pages)
        content.addView(pager, LinearLayout.LayoutParams(-1, dp(360)))
        pageDots = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(0xFFEDEDF2.toInt())
            setPadding(0, dp(4), 0, dp(6))
        }
        content.addView(pageDots, LinearLayout.LayoutParams(-1, dp(32)))
        pager.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            val page = (scrollX / pager.width.coerceAtLeast(1)) + 1
            updateDots(page)
        }
        vertical.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = glassBackground(30).apply {
                setStroke(dp(1), Color.argb(105, 255, 255, 255))
            }
            elevation = dp(16).toFloat()
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        rebuildDock()
        root.addView(vertical, FrameLayout.LayoutParams(-1, -1))
        root.addView(dock, FrameLayout.LayoutParams(-1, dp(88), Gravity.BOTTOM).apply {
            leftMargin = dp(22)
            rightMargin = dp(22)
            bottomMargin = dp(24)
        })
        setContentView(root)
        rebuildPages()
        root.alpha = 0f
        root.scaleX = 0.985f
        root.scaleY = 0.985f
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start()
    }

    private fun rebuildDock() {
        if (!::dock.isInitialized) return
        dock.removeAllViews()
        homePackages.take(4).mapNotNull(::findApp).forEach { app ->
            dock.addView(createIconView(app, dp(62), true),
                LinearLayout.LayoutParams(0, dp(68), 1f))
        }
    }

    private fun rebuildPages() {
        pages.removeAllViews()
        val pageCount = ceil(homePackages.size.coerceAtLeast(1) / 12.0).toInt()
        repeat(pageCount) { pageIndex ->
            val grid = GridLayout(this).apply { columnCount = 4 }
            homePackages.drop(pageIndex * 12).take(12).forEach { packageName ->
                val app = findApp(packageName) ?: return@forEach
                val tile = createAppTile(app)
                tile.setOnLongClickListener { showAppActions(app); true }
                val index = grid.childCount
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(108)
                    columnSpec = GridLayout.spec(index % 4, 1f)
                    rowSpec = GridLayout.spec(index / 4)
                    setMargins(dp(2), dp(5), dp(2), dp(5))
                }
                grid.addView(tile, params)
            }
            pages.addView(grid, LinearLayout.LayoutParams(resources.displayMetrics.widthPixels, -2))
        }
        updateDots(1)
    }

    private fun updateDots(current: Int) {
        val count = ceil(homePackages.size.coerceAtLeast(1) / 12.0).toInt()
        pageDots.text = (1..count).joinToString("   ") { if (it == current) "●" else "○" }
    }

    private fun createAppTile(app: AppInfo): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { launchApp(app.packageName, this) }
        }
        tile.addView(createIconView(app, iconSizePx, false),
            LinearLayout.LayoutParams(iconSizePx, iconSizePx))
        tile.addView(TextView(this).apply {
            text = displayName(app)
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            tag = "app_label"
            setShadowLayer(5f, 0f, 1f, Color.BLACK)
            setPadding(0, dp(6), 0, 0)
        }, LinearLayout.LayoutParams(-1, dp(27)))
        return tile
    }

    private fun createIconView(app: AppInfo, size: Int, dock: Boolean): ImageView {
        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = displayName(app)
            if (dock) setPadding(dp(5), dp(4), dp(5), dp(4))
        }
        loadIcon(app, icon)
        return icon
    }

    private fun loadIcon(app: AppInfo, target: ImageView) {
        worker.execute {
            val custom = prefs.getString("icon_${app.packageName}", null)
            val bitmap = if (custom != null) {
                try { contentResolver.openInputStream(Uri.parse(custom))?.use { BitmapFactory.decodeStream(it) } }
                catch (_: Exception) { null }
            } else iconRenderer.getIcon(app.packageName, iconSizePx)
            mainHandler.post {
                if (isFinishing) return@post
                if (bitmap != null) target.setImageBitmap(bitmap)
                applyIconStyle(target)
            }
        }
    }

    private fun applyIconStyle(view: ImageView) {
        when (prefs.getString("icon_style", "original")) {
            "mono" -> {
                val matrix = ColorMatrix().apply { setSaturation(0f) }
                view.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "blue" -> view.colorFilter = PorterDuffColorFilter(0xFF78A9FF.toInt(), PorterDuff.Mode.SRC_ATOP)
            "rose" -> view.colorFilter = PorterDuffColorFilter(0xFFFFA1C5.toInt(), PorterDuff.Mode.SRC_ATOP)
            "mint" -> view.colorFilter = PorterDuffColorFilter(0xFF7DE2C3.toInt(), PorterDuff.Mode.SRC_ATOP)
            "custom" -> {
                val color = try { Color.parseColor(prefs.getString("icon_custom_color", "#A9B8FF")) }
                    catch (_: Exception) { 0xFFA9B8FF.toInt() }
                view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
            else -> view.clearColorFilter()
        }
    }

    private fun showAppDrawer() {
        if (drawerDialog?.isShowing == true) return
        val theme = prefs.getString("drawer_theme", "amoled") ?: "amoled"
        val bg = when (theme) { "light" -> 0xFFF4F5F8.toInt(); "dark" -> 0xFF17181D.toInt(); else -> Color.BLACK }
        val fg = if (theme == "light") 0xFF171923.toInt() else Color.WHITE
        val hint = if (theme == "light") 0xFF686B75.toInt() else 0xFFB9BBC4.toInt()
        val dialog = Dialog(this)
        drawerDialog = dialog
        val shell = DrawerContainer(this) { dialog.dismiss() }.apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(12)); setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text = "Все приложения"; textSize = 23f; setTypeface(typeface, Typeface.BOLD); setTextColor(fg) }, LinearLayout.LayoutParams(0, dp(48), 1f))
        top.addView(TextView(this).apply { text = "×"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(fg); setOnClickListener { dialog.dismiss() } }, LinearLayout.LayoutParams(dp(44), dp(48)))
        shell.addView(top)
        val search = EditText(this).apply { hint = "Поиск приложений"; setSingleLine(true); textSize = 16f; setTextColor(fg); setHintTextColor(hint); background = rounded(if (theme == "light") 0xFFE5E7ED.toInt() else 0xFF24252B.toInt(), 16); setPadding(dp(14), 0, dp(14), 0) }
        shell.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val grid = GridLayout(this).apply { columnCount = 4 }
        scroll.addView(grid)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(10) })
        fun populate(query: String) {
            grid.removeAllViews()
            apps.filter { displayName(it).contains(query, true) || it.label.contains(query, true) }.forEach { app ->
                val tile = createAppTile(app).apply { setOnClickListener { launchApp(app.packageName, this); dialog.dismiss() }; setOnLongClickListener { showAppActions(app); true } }
                tile.findViewWithTag<TextView>("app_label")?.setTextColor(fg)
                val params = GridLayout.LayoutParams().apply { width = 0; height = dp(108); columnSpec = GridLayout.spec(grid.childCount % 4, 1f); rowSpec = GridLayout.spec(grid.childCount / 4); setMargins(dp(2), dp(5), dp(2), dp(5)) }
                grid.addView(tile, params)
            }
        }
        populate("")
        search.addTextChangedListener(SimpleTextWatcher { populate(it) })
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(shell)
        dialog.window?.apply { setBackgroundDrawableResource(android.R.color.transparent); setDimAmount(0f); setLayout(-1, -1) }
        dialog.setOnDismissListener { drawerDialog = null }
        dialog.show()
        dialog.window?.setLayout(-1, -1)
        shell.alpha = 0f; shell.translationY = dp(30).toFloat(); shell.animate().alpha(1f).translationY(0f).setDuration(220).start()
    }

    private inner class DrawerContainer(context: android.content.Context, private val dismiss: () -> Unit) : LinearLayout(context) {
        private var startX = 0f
        private var startY = 0f
        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) { MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y }; MotionEvent.ACTION_MOVE -> { val dy = event.y - startY; val dx = event.x - startX; if (dy > dp(45) && dy > abs(dx) * 1.2f) return true } }
            return super.onInterceptTouchEvent(event)
        }
        override fun onTouchEvent(event: MotionEvent): Boolean { if (event.actionMasked == MotionEvent.ACTION_UP) { if (event.y - startY > dp(45)) dismiss(); return true }; return true }
    }
    private fun showAppActions(app: AppInfo) {
        val onHome = homePackages.contains(app.packageName)
        val actions = arrayOf(
            if (onHome) "Убрать с главного экрана" else "Добавить на главный экран",
            "Изменить название",
            "Изменить иконку",
            "Удалить приложение"
        )
        AlertDialog.Builder(this).setTitle(displayName(app)).setItems(actions) { _, which ->
            when (which) {
                0 -> {
                    if (onHome) homePackages.remove(app.packageName)
                    else homePackages.add(app.packageName)
                    saveHomeApps()
                    rebuildPages()
                    rebuildDock()
                }
                1 -> renameApp(app)
                2 -> chooseCustomIcon(app)
                3 -> uninstallApp(app)
            }
        }.show()
    }

    private fun renameApp(app: AppInfo) {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(displayName(app))
            setSelection(text.length)
        }
        AlertDialog.Builder(this).setTitle("Название приложения").setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                prefs.edit().putString("name_${app.packageName}", input.text.toString().trim()).apply()
                rebuildPages()
                rebuildDock()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun chooseCustomIcon(app: AppInfo) {
        pendingIconPackage = app.packageName
        val options = arrayOf("Загрузить свою иконку", "Вернуть оригинальную")
        AlertDialog.Builder(this).setTitle("Иконка приложения").setItems(options) { _, which ->
            if (which == 0) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(intent, REQUEST_ICON)
            } else {
                prefs.edit().remove("icon_${app.packageName}").apply()
                rebuildPages()
                rebuildDock()
            }
        }.show()
    }

    @Deprecated("Deprecated by platform API; retained for API 28 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ICON && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) { }
            pendingIconPackage?.let { packageName ->
                prefs.edit().putString("icon_$packageName", uri.toString()).apply()
                rebuildPages()
            }
        }
    }

    private fun uninstallApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
        startActivity(intent)
    }

    private fun showLauncherSettings() { startActivity(Intent(this, SettingsActivity::class.java)) }

    private fun showIconStyles() {
        val styles = arrayOf("Оригинальные", "Монохромные", "Голубой оттенок", "Розовый оттенок", "Мятный оттенок", "Свой цвет…")
        val values = arrayOf("original", "mono", "blue", "rose", "mint")
        AlertDialog.Builder(this).setTitle("Оформление иконок").setItems(styles) { _, which ->
            if (which < values.size) {
                prefs.edit().putString("icon_style", values[which]).apply()
                rebuildPages()
                rebuildDock()
            } else showCustomIconColor()
        }.show()
    }

    private fun showCustomIconColor() {
        val input = EditText(this).apply {
            hint = "#A9B8FF"
            setSingleLine(true)
            setText(prefs.getString("icon_custom_color", "#A9B8FF"))
        }
        AlertDialog.Builder(this).setTitle("Цвет иконок (HEX)").setMessage("Например: #A9B8FF")
            .setView(input).setPositiveButton("Применить") { _, _ ->
                val value = input.text.toString().trim()
                try {
                    Color.parseColor(value)
                    prefs.edit().putString("icon_custom_color", value)
                        .putString("icon_style", "custom").apply()
                    rebuildPages()
                    rebuildDock()
                } catch (_: Exception) {
                    AlertDialog.Builder(this).setMessage("Неверный HEX-цвет").setPositiveButton("ОК", null).show()
                }
            }.setNegativeButton("Отмена", null).show()
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
        try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
        catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun launchApp(packageName: String, source: View) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        source.animate().scaleX(0.88f).scaleY(0.88f).alpha(0.7f).setDuration(90)
            .withEndAction {
                source.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start()
            }.start()
        try {
            val options = ActivityOptions.makeScaleUpAnimation(
                source, 0, 0, source.width.coerceAtLeast(1), source.height.coerceAtLeast(1)
            )
            startActivity(intent, options.toBundle())
        } catch (_: Exception) { startActivity(intent) }
    }

    private fun displayName(app: AppInfo) =
        prefs.getString("name_${app.packageName}", null)?.takeIf { it.isNotBlank() } ?: app.label

    private fun findApp(packageName: String) = apps.firstOrNull { it.packageName == packageName }

    private fun saveHomeApps() {
        prefs.edit().putString("home_apps", homePackages.joinToString("|")).apply()
    }

    private fun glassBackground(radius: Int) = GradientDrawable().apply {
        setColor(Color.argb(72, 235, 240, 255))
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(70, 255, 255, 255))
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

    private class SimpleTextWatcher(private val changed: (String) -> Unit) :
        android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            changed(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }

    companion object {
        private const val REQUEST_ICON = 420
    }
}
