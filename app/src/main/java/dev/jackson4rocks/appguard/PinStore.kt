package dev.jackson4rocks.appguard

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class PinStore(context: Context) {
    private val prefs = context.getSharedPreferences("appguard_security", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.length >= 4 && pin.all(Char::isDigit)) { "PIN must be at least 4 digits" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = hash(pin, salt)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(pin, salt))
    }

    fun lockedPackages(): Set<String> =
        prefs.getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()

    fun setLocked(packageName: String, locked: Boolean) {
        val packages = lockedPackages().toMutableSet()
        if (locked) packages.add(packageName) else packages.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, packages).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_LOCKED = "locked_packages"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
