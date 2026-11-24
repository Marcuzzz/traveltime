package com.example.traveltime.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.traveltime.data.Trip
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExport {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    val HEADER = listOf("date", "start", "end", "duration_minutes", "duration", "mode", "source", "note")

    /** Builds a CSV document (RFC 4180, CRLF line endings) with trips in chronological order. */
    fun build(trips: List<Trip>, zone: ZoneId, nowMillis: Long): String {
        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",")).append("\r\n")
        trips.sortedBy { it.startMillis }.forEach { t ->
            val start = Instant.ofEpochMilli(t.startMillis).atZone(zone)
            val end = t.endMillis?.let { Instant.ofEpochMilli(it).atZone(zone) }
            val durationMs = t.durationMillis(nowMillis)
            val endText = when {
                end == null -> "ongoing"
                end.toLocalDate() != start.toLocalDate() -> "${end.format(dateFmt)} ${end.format(timeFmt)}"
                else -> end.format(timeFmt)
            }
            val row = listOf(
                start.format(dateFmt),
                start.format(timeFmt),
                endText,
                (durationMs / 60_000).toString(),
                formatClock(durationMs).dropLast(3), // h:mm
                t.mode.label,
                if (t.auto) "auto" else "manual",
                t.note,
            )
            sb.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Writes [trips] to a CSV file in the cache and opens the system share sheet. */
    fun share(context: Context, trips: List<Trip>, label: String, nowMillis: Long = System.currentTimeMillis()) {
        val zone = ZoneId.systemDefault()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "traveltime-${LocalDate.now(zone).format(dateFmt)}.csv")
        // BOM so Excel opens non-ASCII notes correctly.
        file.writeText("\uFEFF" + build(trips, zone, nowMillis), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val totalMs = trips.sumOf { it.durationMillis(nowMillis) }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TravelTime – $label")
            putExtra(Intent.EXTRA_TEXT, "Travel time ($label): ${formatDuration(totalMs)} over ${trips.size} trips")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share trips").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
