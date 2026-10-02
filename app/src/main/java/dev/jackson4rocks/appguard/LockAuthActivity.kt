package dev.jackson4rocks.appguard

import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class LockAuthActivity : FragmentActivity() {
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)

        val authenticators =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_WEAK
            }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                sendResult(AppLockAccessibilityService.ACTION_BIOMETRIC_UNLOCKED)
                finish()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                sendResult(AppLockAccessibilityService.ACTION_BIOMETRIC_CANCELLED)
                finish()
            }
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            callback
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock with AppGuard")
            .setSubtitle("Use fingerprint, face unlock, or your screen lock")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        runCatching {
            prompt.authenticate(info)
        }.onFailure {
            sendResult(AppLockAccessibilityService.ACTION_BIOMETRIC_CANCELLED)
            finish()
        }
    }

    private fun sendResult(action: String) {
        sendBroadcast(
            Intent(action)
                .setPackage(packageName)
                .putExtra(
                    AppLockAccessibilityService.EXTRA_PACKAGE,
                    targetPackage
                )
        )
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }
}
