package dev.jackson4rocks.appguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class AppLockAccessibilityService : AccessibilityService() {
    private lateinit var pinStore: PinStore
    private var overlay: View? = null
    private var overlayPackage: String? = null
    private var unlockedPackage: String? = null
    private var lastPackage: String? = null

    private val biometricReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val target = intent.getStringExtra(EXTRA_PACKAGE)
            if (target != null && target == overlayPackage) {
                unlockedPackage = target
                removeOverlay()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        pinStore = PinStore(this)
        ContextCompat.registerReceiver(
            this,
            biometricReceiver,
            IntentFilter(ACTION_BIOMETRIC_UNLOCKED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        val windowEvent =
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED

        if (!windowEvent) return

        if (packageName != lastPackage) {
            if (packageName != unlockedPackage) {
                unlockedPackage = null
            }
            lastPackage = packageName
        }

        if (overlay != null) return
        if (unlockedPackage == packageName) return
        if (!pinStore.hasPin()) return
        if (!pinStore.lockedPackages().contains(packageName)) return

        showLockOverlay(packageName)
    }

    private fun showLockOverlay(packageName: String) {
        if (overlay != null) return
        overlayPackage = packageName

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = rounded(Color.rgb(13, 29, 23), dp(28))
        }

        val shield = TextView(this).apply {
            text = "•"
            gravity = Gravity.CENTER
            textSize = 30f
            setTextColor(Color.rgb(143, 245, 199))
            background = rounded(Color.rgb(24, 54, 43), dp(20))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
        root.addView(
            shield,
            LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                bottomMargin = dp(16)
            }
        )

        val title = TextView(this).apply {
            text = "App locked"
            gravity = Gravity.CENTER
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "Enter your AppGuard PIN to continue"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.rgb(190, 208, 199))
            setPadding(0, dp(8), 0, dp(18))
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val pin = EditText(this).apply {
            hint = "PIN"
            gravity = Gravity.CENTER
            textSize = 18f
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(150, 165, 158))
            background = rounded(Color.rgb(25, 43, 36), dp(18))
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        root.addView(pin, LinearLayout.LayoutParams(-1, dp(58)))

        val unlock = Button(this).apply {
            text = "Unlock"
            textSize = 15f
            setTextColor(Color.rgb(2, 25, 17))
            background = rounded(Color.rgb(143, 245, 199), dp(18))
            isAllCaps = false
            setOnClickListener {
                val target = overlayPackage ?: return@setOnClickListener
                if (pinStore.verify(pin.text.toString())) {
                    unlockedPackage = target
                    removeOverlay()
                } else {
                    pin.text?.clear()
                    Toast.makeText(
                        this@AppLockAccessibilityService,
                        "Incorrect PIN",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        root.addView(
            unlock,
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(12)
            }
        )

        if (pinStore.isBiometricEnabled() && BiometricSupport.canAuthenticate(this)) {
            val biometric = Button(this).apply {
                text = "Use fingerprint / face"
                textSize = 14f
                setTextColor(Color.WHITE)
                background = rounded(Color.rgb(34, 56, 47), dp(18))
                isAllCaps = false
                setOnClickListener {
                    val target = overlayPackage ?: return@setOnClickListener
                    try {
                        startActivity(
                            Intent(
                                this@AppLockAccessibilityService,
                                LockAuthActivity::class.java
                            )
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(LockAuthActivity.EXTRA_PACKAGE, target)
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@AppLockAccessibilityService,
                            "Could not open biometric prompt",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            root.addView(
                biometric,
                LinearLayout.LayoutParams(-1, dp(54)).apply {
                    topMargin = dp(10)
                }
            )
        }

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.62f
        }

        overlay = root

        try {
            wm.addView(root, params)
            pin.requestFocus()
        } catch (_: Exception) {
            overlay = null
            overlayPackage = null
        }
    }

    private fun removeOverlay() {
        val current = overlay ?: return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        runCatching { wm.removeView(current) }
        overlay = null
        overlayPackage = null
    }

    override fun onInterrupt() {
        removeOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        runCatching { unregisterReceiver(biometricReceiver) }
        super.onDestroy()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_BIOMETRIC_UNLOCKED = "dev.jackson4rocks.appguard.BIOMETRIC_UNLOCKED"
        const val EXTRA_PACKAGE = "target_package"
    }
}
