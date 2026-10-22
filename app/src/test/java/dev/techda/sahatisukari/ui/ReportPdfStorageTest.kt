package dev.techda.sahatisukari.ui

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfStorageTest {
    private val validPdf = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n".toByteArray()

    @Test
    fun savesCompletePdfInPrivateReportsDirectory() {
        val directory = Files.createTempDirectory("sahati-report-test").toFile()
        try {
            val file = ReportPdfStorage.save(ByteArrayInputStream(validPdf), validPdf.size.toLong(), directory)

            assertEquals("health-report.pdf", file.name)
            assertArrayEquals(validPdf, file.readBytes())
            assertTrue(file.parentFile == directory)
            assertFalse(java.io.File(directory, ".health-report.pdf.part").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsJsonErrorPayloadAndRemovesPartialFile() {
        val directory = Files.createTempDirectory("sahati-report-test").toFile()
        val jsonError = "{\"error\":\"PDF_UNAVAILABLE\"}".toByteArray()
        try {
            assertThrows(IOException::class.java) {
                ReportPdfStorage.save(ByteArrayInputStream(jsonError), jsonError.size.toLong(), directory)
            }
            assertFalse(java.io.File(directory, "health-report.pdf").exists())
            assertFalse(java.io.File(directory, ".health-report.pdf.part").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsTruncatedPdfAndLengthMismatch() {
        val directory = Files.createTempDirectory("sahati-report-test").toFile()
        val truncated = "%PDF-1.7\nnot finished".toByteArray()
        try {
            assertThrows(IOException::class.java) {
                ReportPdfStorage.save(ByteArrayInputStream(truncated), truncated.size.toLong(), directory)
            }
            assertThrows(IOException::class.java) {
                ReportPdfStorage.save(ByteArrayInputStream(validPdf), validPdf.size.toLong() + 1, directory)
            }
            assertFalse(java.io.File(directory, "health-report.pdf").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
