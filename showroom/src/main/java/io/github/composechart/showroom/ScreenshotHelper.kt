package io.github.composechart.showroom

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {

    /**
     * 捕获指定 View 的截图并异步保存到系统相册（Android 10+ 优先使用 MediaStore，低版本使用沙盒存储）
     */
    fun captureAndSave(
        view: View,
        context: Context,
        fileName: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        try {
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) {
                onComplete(false, "View 尚未绘制完成")
                return
            }

            // 创建 Bitmap 并绘制 View
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            // 异步执行保存操作，避免阻塞 UI 线程
            CoroutineScope(Dispatchers.IO).launch {
                val (success, path) = saveBitmap(context, bitmap, fileName)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "截图已保存: $path", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "截图保存失败: $path", Toast.LENGTH_SHORT).show()
                    }
                    onComplete(success, path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "截图出错: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(false, e.message ?: "未知错误")
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): Pair<Boolean, String> {
        val nameWithExtension = "$fileName.png"
        
        // Android Q (10) 及以上使用 MediaStore 插入到 Pictures 文件夹下，无需运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            
            // 1. 尝试查找并删除同名旧文件，以防产生诸如 "filename (1).png" 的冗余文件
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(nameWithExtension, "Pictures/ComposeChart/")
            
            try {
                resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val deleteUri = android.content.ContentUris.withAppendedId(uri, id)
                        resolver.delete(deleteUri, null, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. 插入新文件
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nameWithExtension)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ComposeChart")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val imageUri = resolver.insert(uri, contentValues)
            if (imageUri != null) {
                try {
                    resolver.openOutputStream(imageUri).use { outputStream ->
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    return Pair(true, "相册/Pictures/ComposeChart/$nameWithExtension")
                } catch (e: Exception) {
                    e.printStackTrace()
                    return Pair(false, e.message ?: "写入 MediaStore 失败")
                }
            }
        }

        // 备用方案（低于 Android 10，或者 MediaStore 失败）：保存到外部沙盒目录，完全不需要动态申请权限
        try {
            val externalDir = context.getExternalFilesDir("screenshots")
            if (externalDir != null) {
                if (!externalDir.exists()) {
                    externalDir.mkdirs()
                }
                val file = File(externalDir, nameWithExtension)
                // 如果已存在则删除
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                return Pair(true, file.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, e.message ?: "写入沙盒目录失败")
        }

        return Pair(false, "无法获取存储路径")
    }
}
