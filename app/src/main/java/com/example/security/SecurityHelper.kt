package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityHelper {

  fun generateSalt(): String {
    val random = SecureRandom()
    val saltBytes = ByteArray(16)
    random.nextBytes(saltBytes)
    return bytesToHex(saltBytes)
  }

  fun hashPin(pin: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val combined = "$salt:$pin".toByteArray(Charsets.UTF_8)
    val hash = digest.digest(combined)
    return bytesToHex(hash)
  }

  fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
    if (expectedHash.isEmpty() || salt.isEmpty()) return false
    val calculated = hashPin(pin, salt)
    return calculated == expectedHash
  }

  private fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
