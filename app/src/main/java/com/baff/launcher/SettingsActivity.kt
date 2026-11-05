package com.baff.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("baff_launcher", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 17, 22)
        window.navigationBarColor = Color.rgb(16, 17, 22)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(24))
            setBackgroundColor(Color.rgb(16, 17, 22))
        }
        root.addView(TextView(this).apply {
            text = "BAFF Launcher"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Настройки лаунчера"
            textSize = 15f
            setTextColor(0xFFB9BBC4.toInt())
            setPadding(0, dp(4), 0, dp(24))
        })
        addSection(root, "ВНЕШНИЙ ВИД")
        addSetting(root, "Стиль иконок", "Оригинальные, монохромные, цветные или свой оттенок") { showIconStyles() }
        addSetting(root, "Тема списка приложений", themeLabel()) { showDrawerThemes() }
        addSection(root, "РАБОЧИЙ СТОЛ")
        addSetting(root, "Выбрать BAFF по умолчанию", "Открыть системный выбор главного экрана") { openHomeSettings() }
        addSetting(root, "Управление приложениями", "Добавление на экран, название и иконки — долгим нажатием на приложение") {
            AlertDialog.Builder(this).setMessage("Открой BAFF Launcher как главный экран, затем открой список приложений свайпом вверх. Удерживай приложение, чтобы изменить его.").setPositiveButton("Понятно", null).show()
        }
        addSection(root, "О ПРОЕКТЕ")
        addSetting(root, "О BAFF Launcher", "Версия 0.4.0 · Бесплатно · Без рекламы") {
            AlertDialog.Builder(this).setTitle("BAFF Launcher 0.4.0").setMessage("Лёгкий домашний экран с сохранением системных обоев.").setPositiveButton("ОК", null).show()
        }
        setContentView(root)
    }

    private fun addSection(root: LinearLayout, title: String) {
        root.addView(TextView(this).apply {
            text = title
            textSize = 11f
            setTextColor(0xFF8D95A8.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(8))
        })
    }

    private fun addSetting(root: LinearLayout, title: String, subtitle: String, click: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF22242C.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(13), dp(16), dp(13))
            setOnClickListener { click() }
        }
        item.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        })
        item.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(0xFFADB1BE.toInt())
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }

    private fun showDrawerThemes() {
        val labels = arrayOf("AMOLED — чистый чёрный", "Тёмная", "Светлая")
        val values = arrayOf("amoled", "dark", "light")
        val current = values.indexOf(prefs.getString("drawer_theme", "amoled")).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Тема списка приложений")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                prefs.edit().putString("drawer_theme", values[which]).apply()
                dialog.dismiss()
                recreate()
            }.setNegativeButton("Закрыть", null).show()
    }

    private fun showIconStyles() {
        val labels = arrayOf("Оригинальные", "Монохромные", "Голубой оттенок", "Розовый оттенок", "Мятный оттенок", "Свой HEX-цвет")
        val values = arrayOf("original", "mono", "blue", "rose", "mint")
        AlertDialog.Builder(this).setTitle("Стиль иконок").setItems(labels) { _, which ->
            if (which < values.size) prefs.edit().putString("icon_style", values[which]).apply()
            else showCustomIconColor()
        }.show()
    }

    private fun showCustomIconColor() {
        val input = EditText(this).apply {
            hint = "#A9B8FF"
            setSingleLine(true)
            setText(prefs.getString("icon_custom_color", "#A9B8FF"))
        }
        AlertDialog.Builder(this).setTitle("Свой цвет иконок").setMessage("Введи HEX-цвет, например #A9B8FF")
            .setView(input).setPositiveButton("Сохранить") { _, _ ->
                val value = input.text.toString().trim()
                try {
                    Color.parseColor(value)
                    prefs.edit().putString("icon_custom_color", value).putString("icon_style", "custom").apply()
                } catch (_: Exception) {
                    AlertDialog.Builder(this).setMessage("Некорректный HEX-цвет").setPositiveButton("ОК", null).show()
                }
            }.setNegativeButton("Отмена", null).show()
    }

    private fun themeLabel() = when (prefs.getString("drawer_theme", "amoled")) {
        "dark" -> "Тёмная"
        "light" -> "Светлая"
        else -> "AMOLED"
    }

    private fun openHomeSettings() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
        catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
