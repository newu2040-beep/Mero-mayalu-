package com.example.core.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.domain.SavedNote
import java.io.OutputStream
import java.time.Instant

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val labelEn: String,
    val labelNe: String
) {
    PDF("pdf", "application/pdf", "PDF Document (.pdf)", "PDF कागजात (.pdf)"),
    CSV("csv", "text/csv", "CSV Spreadsheet (.csv)", "CSV तालिका (.csv)"),
    TXT("txt", "text/plain", "Plain Text (.txt)", "साधारण पाठ (.txt)"),
    JSON("json", "application/json", "JSON Data (.json)", "JSON डेटा (.json)")
}

data class ExportUserData(
    val uid: String,
    val displayName: String,
    val gender: String,
    val age: Int,
    val dateOfBirthPrivate: String,
    val province: String,
    val district: String,
    val municipality: String,
    val culturalBadge: String,
    val bio: String,
    val lookingFor: String,
    val interests: List<String>,
    val prompts: List<String>,
    val outgoingRequestsCount: Int,
    val incomingRequestsCount: Int,
    val activeConversationsCount: Int,
    val savedNotes: List<SavedNote>,
    val exportedAtIso: String = Instant.now().toString()
)

object DataExportHelper {

    fun toJson(data: ExportUserData): String = buildString {
        append("{\n")
        append("  \"uid\": \"${escapeJson(data.uid)}\",\n")
        append("  \"displayName\": \"${escapeJson(data.displayName)}\",\n")
        append("  \"gender\": \"${escapeJson(data.gender)}\",\n")
        append("  \"age\": ${data.age},\n")
        append("  \"dateOfBirthPrivate\": \"${escapeJson(data.dateOfBirthPrivate)}\",\n")
        append("  \"province\": \"${escapeJson(data.province)}\",\n")
        append("  \"district\": \"${escapeJson(data.district)}\",\n")
        append("  \"municipality\": \"${escapeJson(data.municipality)}\",\n")
        append("  \"culturalBadge\": \"${escapeJson(data.culturalBadge)}\",\n")
        append("  \"bio\": \"${escapeJson(data.bio)}\",\n")
        append("  \"lookingFor\": \"${escapeJson(data.lookingFor)}\",\n")
        append("  \"interests\": [${data.interests.joinToString { "\"${escapeJson(it)}\"" }}],\n")
        append("  \"prompts\": [${data.prompts.joinToString { "\"${escapeJson(it)}\"" }}],\n")
        append("  \"outgoingRequestsCount\": ${data.outgoingRequestsCount},\n")
        append("  \"incomingRequestsCount\": ${data.incomingRequestsCount},\n")
        append("  \"activeConversationsCount\": ${data.activeConversationsCount},\n")
        append("  \"savedNotesCount\": ${data.savedNotes.size},\n")
        append("  \"savedNotes\": [\n")
        data.savedNotes.forEachIndexed { idx, note ->
            val comma = if (idx < data.savedNotes.lastIndex) "," else ""
            append("    {\"title\": \"${escapeJson(note.title)}\", \"noteText\": \"${escapeJson(note.noteText)}\", \"sourceAuthor\": \"${escapeJson(note.sourceAuthorName)}\"}$comma\n")
        }
        append("  ],\n")
        append("  \"exportedAt\": \"${escapeJson(data.exportedAtIso)}\"\n")
        append("}")
    }

    fun toCsv(data: ExportUserData): String = buildString {
        appendLine("Section,Field,Value")
        appendCsvRow("Account", "UID", data.uid)
        appendCsvRow("Account", "Display Name", data.displayName)
        appendCsvRow("Account", "Gender", data.gender)
        appendCsvRow("Account", "Age", data.age.toString())
        appendCsvRow("Account", "Date of Birth", data.dateOfBirthPrivate)
        appendCsvRow("Location", "Province", data.province)
        appendCsvRow("Location", "District", data.district)
        appendCsvRow("Location", "Municipality", data.municipality)
        appendCsvRow("Profile", "Cultural Badge", data.culturalBadge)
        appendCsvRow("Profile", "Bio", data.bio)
        appendCsvRow("Profile", "Looking For", data.lookingFor)
        appendCsvRow("Profile", "Interests", data.interests.joinToString("; "))
        data.prompts.forEachIndexed { i, prompt ->
            appendCsvRow("Prompt", "Prompt ${i + 1}", prompt.replace("::", " -> "))
        }
        appendCsvRow("Activity", "Outgoing Requests", data.outgoingRequestsCount.toString())
        appendCsvRow("Activity", "Incoming Requests", data.incomingRequestsCount.toString())
        appendCsvRow("Activity", "Active Conversations", data.activeConversationsCount.toString())
        appendCsvRow("Activity", "Saved Notes", data.savedNotes.size.toString())
        data.savedNotes.forEachIndexed { i, note ->
            appendCsvRow("SavedNote", note.title.ifBlank { "Note ${i + 1}" }, note.noteText)
        }
        appendCsvRow("Metadata", "Exported At", data.exportedAtIso)
    }

    fun toPlainText(data: ExportUserData): String = buildString {
        appendLine("============================================")
        appendLine("MERO MAYALU - PERSONAL DATA EXPORT")
        appendLine("============================================")
        appendLine("Exported At : ${data.exportedAtIso}")
        appendLine("User ID     : ${data.uid}")
        appendLine("Name        : ${data.displayName}")
        appendLine("Gender      : ${data.gender}")
        appendLine("Age         : ${data.age}")
        appendLine("Birth Date  : ${data.dateOfBirthPrivate}")
        appendLine("Location    : ${listOf(data.municipality, data.district, data.province).filter { it.isNotBlank() }.joinToString(", ")}")
        appendLine()
        appendLine("--- PROFILE ---")
        if (data.culturalBadge.isNotBlank()) appendLine("Cultural Badge : ${data.culturalBadge}")
        if (data.bio.isNotBlank()) appendLine("Bio            : ${data.bio}")
        if (data.lookingFor.isNotBlank()) appendLine("Looking For    : ${data.lookingFor}")
        if (data.interests.isNotEmpty()) appendLine("Interests      : ${data.interests.joinToString(", ")}")
        if (data.prompts.isNotEmpty()) {
            appendLine("Prompts:")
            data.prompts.forEach { p -> appendLine("  - ${p.replace("::", ": ")}") }
        }
        appendLine()
        appendLine("--- ACTIVITY SUMMARY ---")
        appendLine("Outgoing Requests   : ${data.outgoingRequestsCount}")
        appendLine("Incoming Requests   : ${data.incomingRequestsCount}")
        appendLine("Active Chats        : ${data.activeConversationsCount}")
        appendLine("Saved Post Notes    : ${data.savedNotes.size}")
        if (data.savedNotes.isNotEmpty()) {
            appendLine()
            appendLine("--- SAVED NOTES ---")
            data.savedNotes.forEachIndexed { i, note ->
                appendLine("${i + 1}. ${note.title.ifBlank { "Note" }}")
                if (note.sourceAuthorName.isNotBlank()) appendLine("   From: ${note.sourceAuthorName}")
                appendLine("   ${note.noteText}")
                appendLine()
            }
        }
    }

    fun writeToStream(
        format: ExportFormat,
        data: ExportUserData,
        outputStream: OutputStream
    ) {
        when (format) {
            ExportFormat.JSON -> outputStream.write(toJson(data).toByteArray(Charsets.UTF_8))
            ExportFormat.CSV -> outputStream.write(toCsv(data).toByteArray(Charsets.UTF_8))
            ExportFormat.TXT -> outputStream.write(toPlainText(data).toByteArray(Charsets.UTF_8))
            ExportFormat.PDF -> writePdfToStream(data, outputStream)
        }
    }

    fun writePdfToStream(data: ExportUserData, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 44f
        val maxTextWidth = pageWidth - (margin * 2)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(122, 24, 58)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 20, 38)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 45)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 105, 112)
            textSize = 9.5f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas
        var y = 56f

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 48f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 52f
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineHeight: Float = 16f) {
            text.split("\n").forEach { paragraph ->
                if (paragraph.isBlank()) {
                    y += lineHeight * 0.6f
                    return@forEach
                }
                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxTextWidth, null)
                    val safeCount = count.coerceAtLeast(1)
                    var cut = safeCount
                    if (safeCount < remaining.length) {
                        val lastSpace = remaining.substring(0, safeCount).lastIndexOf(' ')
                        if (lastSpace > 8) cut = lastSpace
                    }
                    ensureSpace(lineHeight)
                    val line = remaining.substring(0, cut).trimStart()
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                    remaining = remaining.substring(cut).trimStart()
                }
            }
        }

        canvas.drawText("Mero Mayalu — Personal Data Export", margin, y, titlePaint)
        y += 20f
        canvas.drawText("Exported: ${data.exportedAtIso}", margin, y, mutedPaint)
        y += 24f

        drawWrapped("Account & Identity", headingPaint, 20f)
        drawWrapped("Name: ${data.displayName}  |  Gender: ${data.gender}  |  Age: ${data.age}", bodyPaint)
        drawWrapped("Birth Date (Private): ${data.dateOfBirthPrivate}", bodyPaint)
        drawWrapped(
            "Location: ${listOf(data.municipality, data.district, data.province).filter { it.isNotBlank() }.joinToString(", ")}",
            bodyPaint
        )
        y += 12f

        drawWrapped("Cultural Profile", headingPaint, 20f)
        if (data.culturalBadge.isNotBlank()) drawWrapped("Badge: ${data.culturalBadge}", bodyPaint)
        if (data.bio.isNotBlank()) drawWrapped("Bio: ${data.bio}", bodyPaint)
        if (data.lookingFor.isNotBlank()) drawWrapped("Looking For: ${data.lookingFor}", bodyPaint)
        if (data.interests.isNotEmpty()) drawWrapped("Interests: ${data.interests.joinToString(", ")}", bodyPaint)
        if (data.prompts.isNotEmpty()) {
            data.prompts.forEach { p -> drawWrapped("• ${p.replace("::", ": ")}", bodyPaint) }
        }
        y += 12f

        drawWrapped("Connections & Activity", headingPaint, 20f)
        drawWrapped(
            "Outgoing Requests: ${data.outgoingRequestsCount}  |  Incoming Requests: ${data.incomingRequestsCount}  |  Chats: ${data.activeConversationsCount}",
            bodyPaint
        )
        y += 12f

        if (data.savedNotes.isNotEmpty()) {
            drawWrapped("Saved Cultural Notes (${data.savedNotes.size})", headingPaint, 20f)
            data.savedNotes.forEachIndexed { idx, note ->
                drawWrapped("${idx + 1}. ${note.title.ifBlank { "Note" }}", headingPaint, 16f)
                if (note.sourceAuthorName.isNotBlank()) {
                    drawWrapped("From ${note.sourceAuthorName}", mutedPaint, 14f)
                }
                drawWrapped(note.noteText, bodyPaint)
                y += 6f
            }
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    private fun StringBuilder.appendCsvRow(section: String, field: String, value: String) {
        append(csvEscape(section)).append(',')
        append(csvEscape(field)).append(',')
        append(csvEscape(value)).append('\n')
    }

    private fun csvEscape(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace('\n', ' ')
        return if (cleaned.contains(',') || cleaned.contains('"')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
