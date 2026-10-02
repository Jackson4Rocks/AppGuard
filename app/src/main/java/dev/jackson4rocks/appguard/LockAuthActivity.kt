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

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    sendBroadcast(
                        Intent(AppLockAccessibilityService.ACTION_BIOMETRIC_UNLOCKED)
                            .setPackage(packageName)
                            .putExtra(
                                AppLockAccessibilityService.EXTRA_PACKAGE,
                                targetPackage
                            )
                    )
                    finish()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock with AppGuard")
            .setSubtitle("Use fingerprint, face unlock, or your screen lock")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }
}
