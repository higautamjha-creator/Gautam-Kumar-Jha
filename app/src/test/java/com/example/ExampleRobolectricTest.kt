package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentFileType
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.example.data.security.BiometricAuthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DocuVault", appName)
        val welcomeText = context.getString(R.string.welcome_splash_title)
        assertEquals(
            "Welcome to Personal Document App - Developed by Gautam Kumar Jha",
            welcomeText
        )
    }

    @Test
    fun `parse document keywords and category accurately`() {
        val entity = VaultDocumentEntity(
            id = "doc-101",
            ownerUid = "uid-1",
            ownerEmail = "user@gmail.com",
            title = "National Passport",
            category = "IDENTITY",
            fileType = "PDF_DOCUMENT",
            keywords = "passport, travel, identity",
            notes = "Expires 2032",
            localFilePath = "/tmp/passport.pdf",
            fileSizeBytes = 2048L,
            pageCount = 2
        )
        assertEquals(DocumentCategory.IDENTITY, entity.categoryEnum)
        assertEquals(DocumentFileType.PDF_DOCUMENT, entity.fileTypeEnum)
        assertEquals(listOf("passport", "travel", "identity"), entity.keywordList)
        assertTrue(DocumentFileProcessor.formatBytes(2048L).contains("KB"))
    }

    @Test
    fun `verify biometric pin hashing and status detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = BiometricAuthHelper.checkBiometricStatus(context)
        assertNotNull(status)

        val hash1 = BiometricAuthHelper.hashPinSha256("2580")
        val hash2 = BiometricAuthHelper.hashPinSha256("2580")
        val hashDifferent = BiometricAuthHelper.hashPinSha256("1357")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashDifferent)
    }
}
