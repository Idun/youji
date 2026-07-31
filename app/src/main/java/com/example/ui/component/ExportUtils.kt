package com.example.ui.component

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

object ExportUtils {

    fun savePdfToLocal(context: Context, title: String, content: String): String {
        val docTitle = title.trim().ifBlank { "灵感文档" }
        val fileName = "${docTitle}_${System.currentTimeMillis()}.pdf"

        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                color = android.graphics.Color.rgb(30, 30, 30)
                textSize = 14f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = android.graphics.Color.rgb(20, 20, 20)
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 60f
            canvas.drawText(docTitle, 40f, y, titlePaint)
            y += 40f

            val lines = content.split("\n")
            for (line in lines) {
                if (y > 800f) break
                if (line.isBlank()) {
                    y += 18f
                    continue
                }
                var start = 0
                while (start < line.length && y < 800f) {
                    val count = paint.breakText(line, start, line.length, true, 515f, null)
                    val chunk = line.substring(start, start + count)
                    canvas.drawText(chunk, 40f, y, paint)
                    y += 22f
                    start += count
                }
            }

            pdfDocument.finishPage(page)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                    pdfDocument.close()
                    "已成功保存至本地 Downloads/$fileName"
                } else {
                    pdfDocument.close()
                    "保存失败，无法写入下载目录"
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()
                "已成功保存至本地文件 (${file.name})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "保存失败: ${e.message}"
        }
    }

    fun saveImageToLocal(context: Context, title: String, content: String): String {
        val docTitle = title.trim().ifBlank { "灵感文档" }
        val fileName = "${docTitle}_${System.currentTimeMillis()}.png"

        return try {
            val width = 800
            val padding = 40
            val linePaint = Paint().apply {
                color = android.graphics.Color.rgb(44, 44, 44)
                textSize = 28f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = android.graphics.Color.rgb(30, 30, 30)
                textSize = 38f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val lines = content.split("\n")
            var totalHeight = padding * 2 + 70
            for (line in lines) {
                if (line.isBlank()) {
                    totalHeight += 24
                    continue
                }
                var start = 0
                while (start < line.length) {
                    val count = linePaint.breakText(line, start, line.length, true, (width - padding * 2).toFloat(), null)
                    totalHeight += 40
                    start += count
                }
            }
            totalHeight = totalHeight.coerceAtLeast(400)

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.rgb(252, 250, 246))

            var y = (padding + 40).toFloat()
            canvas.drawText(docTitle, padding.toFloat(), y, titlePaint)
            y += 50f

            for (line in lines) {
                if (line.isBlank()) {
                    y += 24f
                    continue
                }
                var start = 0
                while (start < line.length) {
                    val count = linePaint.breakText(line, start, line.length, true, (width - padding * 2).toFloat(), null)
                    val chunk = line.substring(start, start + count)
                    canvas.drawText(chunk, padding.toFloat(), y, linePaint)
                    y += 40f
                    start += count
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    "已成功保存至本地 Pictures/$fileName"
                } else {
                    "保存失败，无法写入图片目录"
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                "已成功保存至本地相册 (${file.name})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "保存失败: ${e.message}"
        }
    }

    fun saveMarkdownToLocal(context: Context, title: String, content: String): String {
        val docTitle = title.trim().ifBlank { "灵感文档" }
        val fileName = "${docTitle}_${System.currentTimeMillis()}.md"
        val mdContent = "# $docTitle\n\n$content"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(mdContent.toByteArray(Charsets.UTF_8))
                    }
                    "已成功保存至本地 Downloads/$fileName"
                } else {
                    "保存失败，无法写入下载目录"
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val file = File(dir, fileName)
                file.writeText(mdContent, Charsets.UTF_8)
                "已成功保存至本地 Downloads/${file.name}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "保存失败: ${e.message}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onDismissRequest: () -> Unit,
    context: Context,
    title: String,
    content: String,
    onResult: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFFF6F5ED),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "导出与本地保存",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            ListItem(
                headlineContent = { Text("保存为 PDF 文档 (.pdf)", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("渲染高品质 PDF 排版文档，直接保存至本地下载目录", fontSize = 12.sp) },
                leadingContent = {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismissRequest()
                        val msg = ExportUtils.savePdfToLocal(context, title, content)
                        onResult(msg)
                    }
            )

            ListItem(
                headlineContent = { Text("保存为 高清长图 (.png)", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("生成复古纸张风格排版图片，直接保存至本地相册", fontSize = 12.sp) },
                leadingContent = {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismissRequest()
                        val msg = ExportUtils.saveImageToLocal(context, title, content)
                        onResult(msg)
                    }
            )

            ListItem(
                headlineContent = { Text("保存为 Markdown 文本 (.md)", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("保存标准 Markdown 格式纯文本文件至本地下载目录", fontSize = 12.sp) },
                leadingContent = {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismissRequest()
                        val msg = ExportUtils.saveMarkdownToLocal(context, title, content)
                        onResult(msg)
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
