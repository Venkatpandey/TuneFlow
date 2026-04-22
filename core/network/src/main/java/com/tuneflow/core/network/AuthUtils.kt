package com.tuneflow.core.network

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

object AuthUtils {
    private val random = SecureRandom()

    fun generateSalt(length: Int = 8): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString {
            repeat(length) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    fun buildToken(
        password: String,
        salt: String,
    ): String {
        val input = password + salt
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }
}
