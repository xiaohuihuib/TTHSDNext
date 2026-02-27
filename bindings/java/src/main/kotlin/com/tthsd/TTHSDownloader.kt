package com.tthsd

import com.google.gson.Gson
import com.sun.jna.Memory
import com.sun.jna.Pointer

/**
 * DownloadEvent - DLL 回调中的事件结构（对应 Rust 中的 Event）
 */
data class DownloadEvent(
    val Type: String,
    val Name: String?,
    val ShowName: String?,
    val ID: String?
)

/**
 * DownloadCallback - 事件回调函数类型
 *
 * @param event 事件元数据
 * @param data  事件附带的额外数据（JSON 反序列化为 Map）
 */
typealias DownloadCallback = (event: DownloadEvent, data: Map<String, Any?>) -> Unit

/**
 * TTHSDownloader
 *
 * TTHSD 高速下载器的 Java/Kotlin 封装类。
 *
 * ## 特性
 * - 支持单文件和多文件批量下载
 * - 支持暂停 / 恢复 / 停止
 * - 通过 [DownloadCallback] 接收进度、事件和错误通知
 * - 在桌面端（Windows/Linux/macOS）通过 JNA 自动加载动态库
 * - 可直接作为 Gradle 依赖添加到 Minecraft Mod/Plugin 或第三方启动器中
 *
 * ## 最简用法
 * ```kotlin
 * val dl = TTHSDownloader()
 * val id = dl.startDownload(
 *     urls = listOf("https://example.com/a.zip"),
 *     savePaths = listOf("/tmp/a.zip"),
 *     callback = { event, data ->
 *         if (event.Type == "update") println(data)
 *     }
 * )
 * ```
 *
 * @param libPath 动态库绝对路径（null 则自动从 JAR 提取或搜索工作目录）
 */
class TTHSDownloader(libPath: String? = null) : AutoCloseable {

    private val lib: TTHSDLibraryJNA = TTHSDLibraryJNA.load(libPath)
    private val gson = Gson()

    /** 保存所有 JNA Callback 引用，防止 GC 提前回收导致 JVM 崩溃 */
    private val callbackRefs = mutableMapOf<Int, TTHSDLibraryJNA.ProgressCallback>()

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    /** 构建 DLL 需要的任务 JSON 字符串 */
    private fun buildTasksJson(
        urls: List<String>,
        savePaths: List<String>,
        showNames: List<String>?,
        ids: List<String>?
    ): String {
        require(urls.size == savePaths.size) {
            "urls 与 savePaths 长度不一致: ${urls.size} vs ${savePaths.size}"
        }
        val tasks = urls.mapIndexed { i, url ->
            mapOf(
                "url"       to url,
                "save_path" to savePaths[i],
                "show_name" to (showNames?.getOrNull(i) ?: url.substringAfterLast('/').substringBefore('?').ifEmpty { "task_$i" }),
                "id"        to (ids?.getOrNull(i) ?: java.util.UUID.randomUUID().toString())
            )
        }
        return gson.toJson(tasks)
    }

    /** 将 Kotlin lambda 封装为 JNA Callback 对象 */
    private fun makeJnaCallback(callback: DownloadCallback): TTHSDLibraryJNA.ProgressCallback {
        return TTHSDLibraryJNA.ProgressCallback { eventJson, dataJson ->
            try {
                @Suppress("UNCHECKED_CAST")
                val event = gson.fromJson(eventJson ?: "{}", DownloadEvent::class.java)
                @Suppress("UNCHECKED_CAST")
                val data = gson.fromJson(dataJson ?: "{}", Map::class.java) as Map<String, Any?>
                callback(event, data)
            } catch (e: Exception) {
                System.err.println("[TTHSD] 回调异常（不影响下载）: ${e.message}")
            }
        }
    }

    /** 构建可选的 bool* 指针（JNA Memory 方式） */
    private fun boolPtr(value: Boolean): Pointer {
        val mem = Memory(1)
        mem.setByte(0, if (value) 1.toByte() else 0.toByte())
        return mem
    }

    // ------------------------------------------------------------------
    // 公开 API
    // ------------------------------------------------------------------

    /**
     * 创建下载器实例，但**不**立即启动。
     * @return 下载器 ID（正整数），失败抛出异常
     */
    fun getDownloader(
        urls: List<String>,
        savePaths: List<String>,
        threadCount: Int = 64,
        chunkSizeMB: Int = 10,
        callback: DownloadCallback? = null,
        useCallbackUrl: Boolean = false,
        userAgent: String? = null,
        remoteCallbackUrl: String? = null,
        useSocket: Boolean? = null,
        showNames: List<String>? = null,
        ids: List<String>? = null
    ): Int {
        val tasksJson = buildTasksJson(urls, savePaths, showNames, ids)
        val cb = callback?.let { makeJnaCallback(it) }

        val id = lib.get_downloader(
            tasksJson, urls.size, threadCount, chunkSizeMB,
            cb,
            useCallbackUrl, userAgent, remoteCallbackUrl,
            useSocket?.let { boolPtr(it) }
        )

        if (id == -1) error("[TTHSD] getDownloader 失败（DLL 返回 -1）")
        if (cb != null) callbackRefs[id] = cb
        return id
    }

    /**
     * 创建并**立即启动**下载器。
     * @return 下载器 ID（正整数），失败抛出异常
     */
    fun startDownload(
        urls: List<String>,
        savePaths: List<String>,
        threadCount: Int = 64,
        chunkSizeMB: Int = 10,
        callback: DownloadCallback? = null,
        useCallbackUrl: Boolean = false,
        userAgent: String? = null,
        remoteCallbackUrl: String? = null,
        useSocket: Boolean? = null,
        isMultiple: Boolean? = null,
        showNames: List<String>? = null,
        ids: List<String>? = null
    ): Int {
        val tasksJson = buildTasksJson(urls, savePaths, showNames, ids)
        val cb = callback?.let { makeJnaCallback(it) }

        val id = lib.start_download(
            tasksJson, urls.size, threadCount, chunkSizeMB,
            cb,
            useCallbackUrl, userAgent, remoteCallbackUrl,
            useSocket?.let { boolPtr(it) },
            isMultiple?.let { boolPtr(it) }
        )

        if (id == -1) error("[TTHSD] startDownload 失败（DLL 返回 -1）")
        if (cb != null) callbackRefs[id] = cb
        return id
    }

    /** 按 ID 顺序启动下载 */
    fun startDownloadById(downloaderId: Int): Boolean =
        lib.start_download_id(downloaderId) == 0

    /** 按 ID 并行启动下载 */
    fun startMultipleDownloadsById(downloaderId: Int): Boolean =
        lib.start_multiple_downloads_id(downloaderId) == 0

    /** 暂停下载 */
    fun pauseDownload(downloaderId: Int): Boolean =
        lib.pause_download(downloaderId) == 0

    /** 恢复下载 */
    fun resumeDownload(downloaderId: Int): Boolean =
        lib.resume_download(downloaderId) == 0

    /** 停止并销毁下载器 */
    fun stopDownload(downloaderId: Int): Boolean {
        val ret = lib.stop_download(downloaderId) == 0
        callbackRefs.remove(downloaderId)
        return ret
    }

    /** 释放所有资源（AutoCloseable 支持，可用于 try-with-resources） */
    override fun close() {
        callbackRefs.clear()
    }
}
