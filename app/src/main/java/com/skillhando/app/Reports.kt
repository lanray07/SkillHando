package com.skillhando.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.DateFormat
import java.util.Date

data class Report(val headings: List<String>, val rows: List<List<String>>, val summary: String)

fun reportFor(attempts: List<Attempt>, skills: List<Skill>, goals: List<Goal>, evidence: List<Evidence>, privateFields: Boolean): Report {
    val headings = listOf(tr("date"), tr("skill"), tr("challenge"), tr("seconds"), tr("status"), tr("self_ratings"), tr("attachments")) +
        if (privateFields) listOf(tr("baseline"), tr("experiment"), tr("observation"), tr("reflection"), tr("next_change"), tr("notes")) else emptyList()
    val dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Localisation.locale)
    val rows = attempts.map { attempt ->
        val skill = skills.find { it.id == attempt.skill }
        listOf(dates.format(Date(attempt.finishedAt)), skill?.title.orEmpty(),
            skill?.challenges?.find { it.id == attempt.challenge }?.title.orEmpty(), attempt.seconds.toString(),
            if (attempt.complete) tr("complete") else tr("incomplete"), attempt.ratingValues,
            evidence.count { it.attemptId == attempt.id }.toString()) +
            if (privateFields) listOf(attempt.baseline, attempt.experiment, attempt.observation, attempt.reflection, attempt.nextChange, attempt.notes)
            else emptyList()
    }
    val complete = attempts.filter { it.complete }
    val milestones = skills.count { skill -> skill.challenges.all { challenge -> complete.any { it.challenge == challenge.id } } }
    val summary = tr("report_name") + "\n" + trf("practice_totals", complete.size, attempts.sumOf { it.seconds } / 60) + "\n" + trf("attachment_count", evidence.size) + "\n" +
        trf("pathway_totals", milestones) + "\n" +
        goals.joinToString("\n") { trf("weekly_target", it.weeklySessions) + if (privateFields) " · ${it.label}" else "" }
    return Report(headings, rows, summary)
}

object ReportWriter {
    fun write(context: Context, uri: Uri, kind: String, report: Report) {
        (context.contentResolver.openOutputStream(uri) ?: throw java.io.IOException("Missing document stream")).use { output ->
            if (kind == "pdf") {
                val pdf = PdfDocument()
                try {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(37, 39, 64); textSize = 11f }
                    var pageNumber = 1
                    var page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    var y = 55f
                    val lines = report.summary.lines() + report.rows.flatMap { listOf("") + report.headings.zip(it).map { (name, entry) -> "$name: $entry" } }
                    lines.flatMap { wrap(it, paint) }.forEach { line ->
                        if (y >= 795) {
                            pdf.finishPage(page)
                            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create())
                            y = 55f
                        }
                        page.canvas.drawText(line, 48f, y, paint)
                        y += 17
                    }
                    pdf.finishPage(page)
                    pdf.writeTo(output)
                } finally {
                    pdf.close()
                }
            } else {
                output.bufferedWriter(Charsets.UTF_8).use { writer ->
                    (listOf(report.headings) + report.rows).forEach { row ->
                        writer.write(row.joinToString(",", transform = ::csv))
                        writer.write("\r\n")
                    }
                }
            }
        }
    }

    fun csv(input: String): String {
        val safe = if (input.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || input.startsWith('\t')) "'$input" else input
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun wrap(line: String, paint: Paint): List<String> {
        var rest = line
        val lines = mutableListOf<String>()
        while (rest.isNotEmpty()) {
            val fit = paint.breakText(rest, true, 499f, null).coerceAtLeast(1)
            val length = if (fit < rest.length) rest.lastIndexOf(' ', fit).takeIf { it > 0 } ?: fit else fit
            lines.add(rest.take(length))
            rest = rest.drop(length).trimStart()
        }
        return lines.ifEmpty { listOf("") }
    }
}
