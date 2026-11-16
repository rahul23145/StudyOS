package com.studyos.cainter.util

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.studyos.cainter.R
import com.studyos.cainter.ui.MainActivity

object OverlayBlockerManager {
    private const val TAG = "OverlayBlockerManager"
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.canDrawOverlays(context)
            } catch (e: Exception) {
                false
            }
        } else {
            true
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    fun openMiuiPermissions(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                openOverlaySettings(context)
            }
        }
    }

    fun isMiuiDevice(): Boolean {
        return try {
            val manufacturer = Build.MANUFACTURER.lowercase()
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
        } catch (e: Exception) {
            false
        }
    }

    fun showOverlay(
        context: Context,
        blockedAppName: String,
        taskTitle: String,
        remainingTime: String
    ) {
        if (!canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        mainHandler.post {
            if (isOverlayShowing && overlayView != null) {
                updateOverlayContent(blockedAppName, taskTitle, remainingTime)
                return@post
            }

            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                val themedContext = ContextThemeWrapper(context, R.style.Theme_StudyOS)
                val inflater = LayoutInflater.from(themedContext)
                val view = inflater.inflate(R.layout.overlay_blocked_app, null)

                view.findViewById<TextView>(R.id.tvBlockedAppBanner)?.text = "$blockedAppName is blocked!"
                view.findViewById<TextView>(R.id.tvOverlayTaskTitle)?.text = taskTitle
                view.findViewById<TextView>(R.id.tvOverlayRemainingTime)?.text = remainingTime

                view.findViewById<Button>(R.id.btnOverlayBackToStudy)?.setOnClickListener {
                    hideOverlay(context)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("NAVIGATE_TO", "TIMER")
                    }
                    context.startActivity(intent)
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                windowManager.addView(view, params)
                overlayView = view
                isOverlayShowing = true
                Log.d(TAG, "Overlay successfully added to WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add overlay view", e)
            }
        }
    }

    private fun updateOverlayContent(
        blockedAppName: String,
        taskTitle: String,
        remainingTime: String
    ) {
        overlayView?.findViewById<TextView>(R.id.tvBlockedAppBanner)?.text = "$blockedAppName is blocked!"
        overlayView?.findViewById<TextView>(R.id.tvOverlayTaskTitle)?.text = taskTitle
        overlayView?.findViewById<TextView>(R.id.tvOverlayRemainingTime)?.text = remainingTime
    }

    fun hideOverlay(context: Context) {
        mainHandler.post {
            if (!isOverlayShowing || overlayView == null) return@post
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                windowManager?.removeView(overlayView)
                Log.d(TAG, "Overlay removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            } finally {
                overlayView = null
                isOverlayShowing = false
            }
        }
    }
}
