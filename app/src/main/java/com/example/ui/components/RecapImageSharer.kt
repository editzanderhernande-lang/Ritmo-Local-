package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.stats.HistoryPeriodSnapshot
import com.example.data.stats.MusicalMilestone
import com.example.data.stats.RitmoRecapReport
import com.example.data.stats.RitmoWrappedReport
import com.example.model.formatMonthLabel
import com.example.model.formatPlaybackTimeReadable
import java.io.File
import java.io.FileOutputStream

object RecapImageSharer {

    fun shareRecapCard(
        context: Context,
        report: RitmoRecapReport,
        accentColorArgb: Int,
        secondaryColorArgb: Int,
        localeTag: String = "es-ES"
    ) {
        val monthTitle = formatMonthLabel(report.monthKey, localeTag)
        val lines = buildList {
            report.topArtist?.let { add("Artista #1: ${it.artistName}") }
            report.topSong?.let { add("Canción #1: ${it.trackTitle}") }
            report.topAlbum?.let { add("Álbum #1: ${it.albumName}") }
            add("Tiempo real: ${formatPlaybackTimeReadable(report.totalSeconds)}")
            add("Reproducciones: ${report.totalPlays}")
            report.topGenres.firstOrNull()?.let { add("Género principal: ${it.genre}") }
        }
        val bitmap = renderSummaryBitmap(
            headerBadge = "RITMO RECAP",
            headline = monthTitle,
            subtitle = "${report.uniqueArtistsCount} artistas · ${report.uniqueSongsCount} canciones",
            highlightRows = lines,
            footerNote = "Generado con Ritmo Local",
            accentArgb = accentColorArgb,
            secondaryArgb = secondaryColorArgb
        )
        launchShareSheet(context, bitmap, "ritmo_recap_${report.monthKey}.png", "Mi Ritmo Recap · $monthTitle")
    }

    fun shareWrappedCard(
        context: Context,
        report: RitmoWrappedReport,
        accentColorArgb: Int,
        secondaryColorArgb: Int
    ) {
        val lines = buildList {
            report.topArtist?.let { add("Artista del año: ${it.artistName}") }
            report.topSong?.let { add("Canción del año: ${it.trackTitle}") }
            report.topAlbum?.let { add("Álbum principal: ${it.albumName}") }
            add("Tiempo total: ${formatPlaybackTimeReadable(report.totalSeconds)}")
            add("Reproducciones: ${report.totalPlays}")
            report.discoveryOfYearArtist?.let { add("Descubrimiento: $it") }
        }
        val bitmap = renderSummaryBitmap(
            headerBadge = "RITMO WRAPPED ${report.year}",
            headline = "Resumen ${report.year}",
            subtitle = "${report.uniqueArtistsCount} artistas · ${report.uniqueSongsCount} canciones",
            highlightRows = lines,
            footerNote = "Archivo musical personal · Ritmo Local",
            accentArgb = accentColorArgb,
            secondaryArgb = secondaryColorArgb
        )
        launchShareSheet(context, bitmap, "ritmo_wrapped_${report.year}.png", "Mi Ritmo Wrapped ${report.year}")
    }

    fun shareHistoryMomentCard(
        context: Context,
        snapshot: HistoryPeriodSnapshot,
        milestone: MusicalMilestone? = null,
        accentColorArgb: Int,
        secondaryColorArgb: Int,
        localeTag: String = "es-ES"
    ) {
        val periodTitle = formatMonthLabel(snapshot.monthKey, localeTag)
        val lines = buildList {
            snapshot.topArtistName?.let { add("Artista #1: $it") }
            snapshot.topSongTitle?.let { add("Canción #1: $it") }
            snapshot.topAlbumName?.let { add("Álbum #1: $it") }
            add("Escucha total: ${formatPlaybackTimeReadable(snapshot.totalSeconds)}")
            snapshot.topGenre?.let { add("Género destacado: $it") }
            milestone?.let { add("Hito: ${it.title}") }
        }
        val bitmap = renderSummaryBitmap(
            headerBadge = "MI HISTORIA MUSICAL",
            headline = periodTitle,
            subtitle = "${snapshot.totalPlays} reproducciones registradas",
            highlightRows = lines,
            footerNote = "Recuerdo musical · Ritmo Local",
            accentArgb = accentColorArgb,
            secondaryArgb = secondaryColorArgb
        )
        launchShareSheet(context, bitmap, "ritmo_historia_${snapshot.monthKey}.png", "Mi Historia Musical · $periodTitle")
    }

    fun renderSummaryBitmap(
        headerBadge: String,
        headline: String,
        subtitle: String,
        highlightRows: List<String>,
        footerNote: String,
        accentArgb: Int,
        secondaryArgb: Int
    ): Bitmap {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0B0A14.toInt(), blendDark(accentArgb), blendDark(secondaryArgb), 0xFF0A0912.toInt()),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 19, 17, 32)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.argb(140, Color.red(accentArgb), Color.green(accentArgb), Color.blue(accentArgb))
        }
        val cardRect = RectF(64f, 84f, width - 64f, height - 84f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(headerBadge, 120f, 196f, badgePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headline.take(26), 120f, 290f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4D0EC.toInt()
            textSize = 34f
        }
        canvas.drawText(subtitle.take(48), 120f, 354f, subPaint)

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
        }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var currentY = 440f
        highlightRows.take(6).forEach { line ->
            val rowRect = RectF(112f, currentY, width - 112f, currentY + 114f)
            canvas.drawRoundRect(rowRect, 28f, 28f, pillPaint)
            canvas.drawText(line.take(44), 146f, currentY + 70f, rowPaint)
            currentY += 138f
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9F99C4.toInt()
            textSize = 30f
        }
        canvas.drawText(footerNote, 120f, height - 134f, footerPaint)
        return bitmap
    }

    private fun launchShareSheet(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared_recap_cards").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun blendDark(argb: Int): Int {
        val r = (Color.red(argb) * 0.34f).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * 0.34f).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * 0.42f).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
