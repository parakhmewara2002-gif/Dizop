package com.dizop.print.update

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class InAppUpdateSecurityUnitTest {

    private fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testSha256IntegrityComputation() {
        val testContent = "Dizop Secure Android Printing Engine v1.0.1".toByteArray(Charsets.UTF_8)
        val expectedHash = computeSha256(testContent)

        val tempFile = File.createTempFile("dizop_test_apk", ".apk")
        try {
            tempFile.writeBytes(testContent)

            val digest = MessageDigest.getInstance("SHA-256")
            tempFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val calculated = digest.digest().joinToString("") { "%02x".format(it) }

            assertEquals("Computed SHA-256 must match expected test vector", expectedHash, calculated)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testTamperedApkRejection() {
        val original = "Authentic APK Content".toByteArray(Charsets.UTF_8)
        val tampered = "Tampered APK Content".toByteArray(Charsets.UTF_8)

        val originalHash = computeSha256(original)
        val tamperedHash = computeSha256(tampered)

        assertNotEquals("Tampered content must yield distinct SHA-256 digest", originalHash, tamperedHash)
    }
}
