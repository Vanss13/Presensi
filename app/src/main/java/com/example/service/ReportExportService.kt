package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportService {

    fun exportAttendancePdf(
        context: Context,
        organization: Organization,
        filterLabel: String,
        records: List<AttendanceRecord>
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(dir, "HADIRIN_Laporan_${timestamp}.pdf")

            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f

            val titlePaint = Paint().apply {
                color = Color.parseColor("#172554")
                textSize = 18f
                isFakeBoldText = true
            }
            val subPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 10f
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#172554")
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
            }
            val cellPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 9f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            val rowsPerPage = 24
            val chunks = if (records.isEmpty()) listOf(emptyList()) else records.chunked(rowsPerPage)

            chunks.forEachIndexed { pageIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                var y = 44f
                val orgType = OrganizationType.fromRaw(organization.type)
                canvas.drawText("LAPORAN KEHADIRAN — ${organization.name}", margin, y, titlePaint)
                y += 16f
                canvas.drawText("${orgType.moduleName} | Alamat: ${organization.address} | Telp: ${organization.phone}", margin, y, subPaint)
                y += 14f
                canvas.drawText("Filter: $filterLabel | Total Data: ${records.size} | Halaman ${pageIndex + 1}/${chunks.size}", margin, y, subPaint)
                y += 16f

                val hadirCount = records.count { it.status == "HADIR" }
                val lateCount = records.count { it.status == "TERLAMBAT" }
                val leaveCount = records.count { it.status == "IZIN" || it.status == "SAKIT" }
                canvas.drawText("Ringkasan: Hadir=$hadirCount  •  Terlambat=$lateCount  •  Izin/Sakit=$leaveCount", margin, y, cellPaint)
                y += 18f

                // Table header
                canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBgPaint)
                val headerY = y + 15f
                canvas.drawText("Tanggal", margin + 6f, headerY, headerTextPaint)
                canvas.drawText("Nama Anggota", margin + 74f, headerY, headerTextPaint)
                canvas.drawText("Unit", margin + 204f, headerY, headerTextPaint)
                canvas.drawText("Masuk", margin + 294f, headerY, headerTextPaint)
                canvas.drawText("Pulang", margin + 348f, headerY, headerTextPaint)
                canvas.drawText("Metode", margin + 404f, headerY, headerTextPaint)
                canvas.drawText("Status", margin + 464f, headerY, headerTextPaint)
                y += 26f

                if (chunk.isEmpty()) {
                    canvas.drawText("Belum ada data presensi pada rentang filter ini.", margin + 6f, y + 14f, subPaint)
                } else {
                    chunk.forEach { rec ->
                        val rowY = y + 14f
                        canvas.drawText(rec.dateKey.take(10), margin + 6f, rowY, cellPaint)
                        canvas.drawText(rec.memberName.take(22), margin + 74f, rowY, cellPaint)
                        canvas.drawText((rec.unitName ?: "-").take(14), margin + 204f, rowY, cellPaint)
                        canvas.drawText(rec.checkInTimeText.take(8), margin + 294f, rowY, cellPaint)
                        canvas.drawText((rec.checkOutTimeText ?: "-").take(8), margin + 348f, rowY, cellPaint)
                        canvas.drawText(rec.method.take(8), margin + 404f, rowY, cellPaint)
                        canvas.drawText(rec.status.take(10), margin + 464f, rowY, cellPaint)
                        y += 22f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    }
                }

                pdf.finishPage(page)
            }

            FileOutputStream(outFile).use { fos ->
                pdf.writeTo(fos)
            }
            pdf.close()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAttendanceCsv(
        context: Context,
        organization: Organization,
        records: List<AttendanceRecord>
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(dir, "HADIRIN_Rekap_${timestamp}.csv")

            val sb = StringBuilder()
            // UTF-8 BOM so Microsoft Excel opens Indonesian characters cleanly
            sb.append('\uFEFF')
            sb.appendLine("Organisasi,${escapeCsv(organization.name)}")
            sb.appendLine("Jenis,${escapeCsv(organization.type)}")
            sb.appendLine("Tanggal,Kode Anggota,Nama Anggota,Kelas/Departemen,Jam Masuk,Jam Pulang,Metode,Jarak GPS (m),Status,Terlambat (menit),Jam Kerja (menit),Lembur (menit)")
            records.forEach { r ->
                sb.appendLine(
                    listOf(
                        r.dateKey,
                        r.memberCode,
                        r.memberName,
                        r.unitName ?: "-",
                        r.checkInTimeText,
                        r.checkOutTimeText ?: "-",
                        r.method,
                        r.distanceMeters.toString(),
                        r.status,
                        r.lateMinutes.toString(),
                        r.workMinutes.toString(),
                        r.overtimeMinutes.toString()
                    ).joinToString(",") { escapeCsv(it) }
                )
            }
            outFile.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareReportFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Kehadiran HADIRIN - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Bagikan Laporan HADIRIN")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
