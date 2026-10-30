
package ru.gs8triple

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {

    private data class AppEntry(
        val label: String,
        val packageName: String,
        val activity: String
    ) {
        override fun toString() = label
    }

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var status: TextView
    private lateinit var diagnostics: TextView
    private lateinit var appCount: TextView

    private var apps: List<AppEntry> = emptyList()
    private val spinners = mutableListOf<Spinner>()

    private val packageKeys = listOf(
        "left_package",
        "center_package",
        "right_package"
    )

    private val defaultPackages = listOf(
        "ru.yandex.music",
        "ru.yandex.yandexnavi",
        "ru.rutube.app"
    )

    private val slotNames = listOf(
        "СЛЕВА — музыка • 400 px",
        "ПО ЦЕНТРУ — навигатор • 760 px",
        "СПРАВА — третье приложение • 760 px"
    )

    private val bounds = listOf(
        Rect(0, 0, 400, 968),
        Rect(400, 0, 1160, 968),
        Rect(1160, 0, 1920, 968)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(
            "gs8_triple_settings",
            MODE_PRIVATE
        )

        apps = loadApps()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
        }

        root.addView(TextView(this).apply {
            text = "GS8 TRIPLE"
            textSize = 28f
        })

        root.addView(TextView(this).apply {
            text = "GAC GS8 Gen2 • Android 9 • 1920×968"
            textSize = 14f
        })

        root.addView(section("РАСКЛАДКА"))

        root.addView(TextView(this).apply {
            text = "Слева: 400 px | Центр: 760 px | Справа: 760 px"
            textSize = 14f
        })

        root.addView(section("ВЫБОР ПРИЛОЖЕНИЙ"))

        appCount = TextView(this).apply {
            textSize = 12f
        }
        root.addView(appCount)

        repeat(3) { i ->
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10, 8, 10, 8)
                setBackgroundColor(0xFFEFEFEF.toInt())
            }

            panel.addView(TextView(this).apply {
                text = slotNames[i]
                textSize = 15f
            })

            val spinner = Spinner(this)
            spinners.add(spinner)
            panel.addView(spinner)
            root.addView(panel)
        }

        root.addView(Button(this).apply {
            text = "Обновить список приложений"
            setOnClickListener {
                apps = loadApps()
                populateSpinners()
                status.text = "Список приложений обновлён."
            }
        })

        root.addView(Button(this).apply {
            text = "Сохранить назначения"
            setOnClickListener {
                saveSelections()
                status.text = "Назначения сохранены."
            }
        })

        root.addView(section("РАЗМЕЩЕНИЕ ОКОН"))

        root.addView(TextView(this).apply {
            text = "Запуск с запросом режима Freeform (windowingMode 5) и заданными границами окон."
            textSize = 12f
        })

        root.addView(Button(this).apply {
            text = "Разложить три окна"
            setOnClickListener {
                saveSelections()
                arrangeWindows()
            }
        })

        root.addView(Button(this).apply {
            text = "Проверить поддержку Freeform"
            setOnClickListener {
                runDiagnostics()
            }
        })

        diagnostics = TextView(this).apply {
            text = "Нажми «Проверить поддержку Freeform»."
            textSize = 12f
        }
        root.addView(diagnostics)

        status = TextView(this).apply {
            text = "\nВыбери три приложения."
            textSize = 14f
        }
        root.addView(status)

        setContentView(
            ScrollView(this).apply {
                addView(root)
            }
        )

        populateSpinners()
    }

    private fun section(s: String) =
        TextView(this).apply {
            text = "\n$s"
            textSize = 18f
            setPadding(0, 10, 0, 5)
        }

    @Suppress("DEPRECATION")
    private fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                AppEntry(
                    it.loadLabel(packageManager).toString(),
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun populateSpinners() {
        for (i in spinners.indices) {
            spinners[i].adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    apps
                ).also {
                    it.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )
                }

            val saved = prefs.getString(
                packageKeys[i],
                defaultPackages[i]
            )

            val index = apps.indexOfFirst {
                it.packageName == saved
            }

            spinners[i].setSelection(
                if (index >= 0) index else 0
            )
        }

        appCount.text =
            "Найдено запускаемых приложений: ${apps.size}"
    }

    private fun saveSelections() {
        val edit = prefs.edit()

        spinners.forEachIndexed { i, spinner ->
            apps.getOrNull(spinner.selectedItemPosition)?.let {
                edit.putString(packageKeys[i], it.packageName)
            }
        }

        edit.apply()
    }

    private fun selectedApps(): List<AppEntry> =
        spinners.mapNotNull { spinner ->
            apps.getOrNull(spinner.selectedItemPosition)
        }

    /**
     * Экспериментальный запрос Freeform через скрытый API Android.
     * Наличие метода и успешный вызов ещё не гарантируют,
     * что оконный менеджер примет запрошенный режим.
     */
    private fun createFreeformOptions(
        rect: Rect
    ): Pair<Bundle, String> {

        val options = ActivityOptions.makeBasic()

        val method = ActivityOptions::class.java.getDeclaredMethod(
            "setLaunchWindowingMode",
            Int::class.javaPrimitiveType!!
        )

        method.isAccessible = true
        method.invoke(options, 5)

        options.setLaunchBounds(rect)

        return Pair(
            options.toBundle(),
            "setLaunchWindowingMode(5): вызов выполнен; bounds=$rect"
        )
    }

    private fun arrangeWindows() {
        val chosen = selectedApps()

        if (chosen.size != 3) {
            status.text =
                "Не удалось определить выбранные приложения."
            return
        }

        val duplicate = chosen
            .groupBy { it.packageName }
            .filterValues { it.size > 1 }
            .keys

        if (duplicate.isNotEmpty()) {
            status.text =
                "Выбери три разных приложения."
            return
        }

        val report = StringBuilder()
        var launched = 0

        chosen.forEachIndexed { index, app ->

            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)

                    component = android.content.ComponentName(
                        app.packageName,
                        app.activity
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    )
                }

                val (bundle, message) =
                    createFreeformOptions(bounds[index])

                startActivity(intent, bundle)

                launched++

                report.append(
                    "OK: ${app.label}\n"
                )
                report.append(
                    "$message\n\n"
                )

            } catch (e: Exception) {

                val cause = e.cause ?: e

                report.append(
                    "ОШИБКА: ${app.label}\n"
                )
                report.append(
                    "${cause.javaClass.simpleName}: ${cause.message}\n\n"
                )
            }
        }

        diagnostics.text = report.toString()

        status.text =
            "Отправлено запросов запуска: $launched из 3. Проверь расположение окон."
    }

    @Suppress("DEPRECATION")
    private fun runDiagnostics() {
        val report = StringBuilder()

        report.append(
            "Android API: ${android.os.Build.VERSION.SDK_INT}\n"
        )

        report.append(
            "Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n"
        )

        report.append(
            "FEATURE_FREEFORM_WINDOW_MANAGEMENT: " +
                "${packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)}\n"
        )

        report.append(
            "FEATURE_PICTURE_IN_PICTURE: " +
                "${packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)}\n"
        )

        val freeformSetting = try {
            Settings.Global.getInt(
                contentResolver,
                "enable_freeform_support",
                0
            )
        } catch (_: Exception) {
            -1
        }

        val forceResizable = try {
            Settings.Global.getInt(
                contentResolver,
                "force_resizable_activities",
                0
            )
        } catch (_: Exception) {
            -1
        }

        report.append("enable_freeform_support=$freeformSetting\n")
        report.append("force_resizable_activities=$forceResizable\n")

        try {
            ActivityOptions::class.java.getDeclaredMethod(
                "setLaunchWindowingMode",
                Int::class.javaPrimitiveType!!
            )

            report.append(
                "setLaunchWindowingMode: метод найден\n"
            )

        } catch (e: Exception) {
            report.append(
                "setLaunchWindowingMode: ${e.javaClass.simpleName}: ${e.message}\n"
            )
        }

        val metrics = DisplayMetrics()

        (getSystemService(WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
            .getRealMetrics(metrics)

        report.append(
            "Экран: ${metrics.widthPixels}×${metrics.heightPixels}\n"
        )

        report.append(
            "\nПримечание: наличие метода не гарантирует принятие Freeform оконным менеджером."
        )

        diagnostics.text = report.toString()
        status.text = "Диагностика завершена."
    }
}


 
     
