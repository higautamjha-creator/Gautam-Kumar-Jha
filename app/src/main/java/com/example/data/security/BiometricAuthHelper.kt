package com.example.data.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

enum class BiometricHardwareStatus(
    val title: String,
    val subtitle: String,
    val canPromptDirectly: Boolean
) {
    READY(
        title = "Fingerprint & Face Unlock Active",
        subtitle = "Hardware biometric sensor verified and ready",
        canPromptDirectly = true
    ),
    NOT_ENROLLED(
        title = "No Fingerprint or Face Enrolled",
        subtitle = "Biometric sensor detected, but no fingerprint/face is enrolled in Android Settings yet",
        canPromptDirectly = false
    ),
    NO_HARDWARE(
        title = "Biometric Sensor Unavailable",
        subtitle = "This device or emulator does not report a hardware fingerprint/face sensor. Use your Vault PIN below.",
        canPromptDirectly = false
    ),
    UNAVAILABLE(
        title = "Biometric Sensor Busy",
        subtitle = "Biometric hardware is temporarily unavailable. Use your Vault PIN or try again.",
        canPromptDirectly = false
    )
}

object BiometricAuthHelper {

    fun checkBiometricStatus(context: Context): BiometricHardwareStatus {
        return try {
            val manager = BiometricManager.from(context)
            val result = manager.canAuthenticate(
                Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
            )
            when (result) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
                else -> BiometricHardwareStatus.UNAVAILABLE
            }
        } catch (_: Exception) {
            BiometricHardwareStatus.UNAVAILABLE
        }
    }

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }

    fun showBiometricPrompt(
        context: Context,
        title: String = "Unlock DocuVault",
        subtitle: String = "Verify your fingerprint or face to access your encrypted documents",
        onSuccess: () -> Unit,
        onUsePinFallback: () -> Unit,
        onErrorMessage: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            onErrorMessage("Unable to attach biometric prompt to current activity window.")
            return
        }

        val status = checkBiometricStatus(context)
        if (status == BiometricHardwareStatus.NOT_ENROLLED) {
            onErrorMessage("No fingerprint or face is enrolled on this device yet. Enroll in Settings or use your 4-digit Vault PIN.")
            return
        }
        if (status == BiometricHardwareStatus.NO_HARDWARE) {
            onErrorMessage("No hardware biometric sensor detected on this device. Please use your 4-digit Vault PIN below.")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onUsePinFallback()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> {
                        // User dismissed the prompt; allow retry or PIN
                    }
                    else -> onErrorMessage(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onErrorMessage("Biometric not recognized. Try again or use your Vault PIN.")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
                )
                .setNegativeButtonText("Use Vault PIN")
                .setConfirmationRequired(false)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onErrorMessage("Biometric prompt error: ${e.localizedMessage ?: "Unavailable"}")
        }
    }

    fun openBiometricEnrollmentSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
                    )
                }
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun hashPinSha256(pin: String): String {
        val salted = "docuvault_biometric_pin_v1:${pin.trim()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
