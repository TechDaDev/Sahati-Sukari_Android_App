package dev.techda.sahatisukari.ui

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

internal object ReportPdfStorage {
    fun save(input: InputStream, expectedLength: Long, directory: File): File {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("تعذر حفظ ملف التقرير داخل مساحة التطبيق.")
        }

        val destination = File(directory, "health-report.pdf")
        val partial = File(directory, ".health-report.pdf.part")
        destination.delete()
        partial.delete()

        try {
            val copied = input.use { source -> partial.outputStream().use { source.copyTo(it) } }
            if (copied < PDF_HEADER.size + PDF_EOF.size) throw IOException("ملف التقرير غير مكتمل.")
            if (expectedLength >= 0 && copied != expectedLength) throw IOException("تنزيل ملف التقرير غير مكتمل.")
            validatePdf(partial)
            if (!partial.renameTo(destination)) throw IOException("تعذر حفظ ملف التقرير داخل مساحة التطبيق.")
            return destination
        } catch (failure: Exception) {
            partial.delete()
            destination.delete()
            throw failure
        }
    }

    private fun validatePdf(file: File) {
        RandomAccessFile(file, "r").use { pdf ->
            val header = ByteArray(PDF_HEADER.size)
            pdf.readFully(header)
            if (!header.contentEquals(PDF_HEADER)) throw IOException("استجابة الخادم ليست ملف PDF صالحًا.")

            val tailSize = minOf(pdf.length(), 2048L).toInt()
            val tail = ByteArray(tailSize)
            pdf.seek(pdf.length() - tailSize)
            pdf.readFully(tail)
            if (!tail.toString(Charsets.ISO_8859_1).contains("%%EOF")) {
                throw IOException("ملف التقرير غير مكتمل.")
            }
        }
    }

    private val PDF_HEADER = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val PDF_EOF = "%%EOF".toByteArray(Charsets.US_ASCII)
}
