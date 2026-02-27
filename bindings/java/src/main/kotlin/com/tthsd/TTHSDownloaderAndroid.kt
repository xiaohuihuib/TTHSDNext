package com.tthsd

import com.google.gson.Gson

/**
 * TTHSDownloaderAndroid - Android 平台封装类
 *
 * 使用 JNI（JNA 的 Android 替代方案）调用 android_export.rs 导出的本地方法。
 * 注意：回调进度事件需通过远程 WebSocket/Socket URL 接收（Android JNI 接口不支持函数指针回调）。
 *
 * 使用方式（在 Application 初始化时）：
 * ```kotlin
 * TTHSDLibraryJNI.load()  // System.loadLibrary("TTHSD")
 *
 * val dl = TTHSDownloaderAndroid()
 * val id = dl.startDownload(
 *     urls = listOf("https://example.com/a.zip"),
 *     savePaths = listOf("/sdcard/Download/a.zip"),
 *     callbackUrl = "ws://192.168.1.100:8080",
 *     useSocket = false
 * )
 * ```
 */
class TTHSDownloaderAndroid : AutoCloseable {

    private val gson = Gson()
    private val activeIds = mutableListOf<Int>()

    init {
        TTHSDLibraryJNI.load()
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    private fun buildTasksJson(
        urls: List<String>,
        savePaths: List<String>,
        showNames: List<String>? = null,
        ids: List<String>? = null
    ): String {
        require(urls.size == savePaths.size) {
            "urls 与 savePaths 长度不一致: ${urls.size} vs ${savePaths.size}"
        }
        val tasks = urls.mapIndexed { i, url ->
            mapOf(
                "url"       to url,
                "save_path" to savePaths[i],
                "show_name" to (showNames?.getOrNull(i)
                    ?: url.substringAfterLast('/').substringBefore('?').ifEmpty { "task_$i" }),
                "id"        to (ids?.getOrNull(i) ?: java.util.UUID.randomUUID().toString())
            )
        }
        return gson.toJson(tasks)
    }

    // ------------------------------------------------------------------
    // 公开 API
    // ------------------------------------------------------------------

    /**
     * 创建并立即启动下载器
     *
     * Android 端不支持 C 函数指针回调，请通过 [callbackUrl] 指定 WebSocket/Socket 服务地址接收进度事件。
     *
     * @param callbackUrl  可选的 WebSocket（ws://）或 TCP Socket 地址
     * @param useSocket    true=TCP Socket，false=WebSocket
     * @return 下载器 ID
     */
    fun startDownload(
        urls: List<String>,
        savePaths: List<String>,
        threadCount: Int = 64,
        chunkSizeMB: Int = 10,
        callbackUrl: String = "",
        useSocket: Boolean = false,
        isMultiple: Boolean = false,
        showNames: List<String>? = null,
        ids: List<String>? = null
    ): Int {
        val tasksJson = buildTasksJson(urls, savePaths, showNames, ids)
        val useCallbackUrl = callbackUrl.isNotEmpty()

        val id = TTHSDLibraryJNI.startDownload(
            tasksJson, threadCount, chunkSizeMB,
            useCallbackUrl, callbackUrl, useSocket, isMultiple
        )
        if (id == -1) error("[TTHSD] startDownload 失败（JNI 返回 -1）")
        activeIds += id
        return id
    }

    /**
     * 创建下载器（不立即启动）
     */
    fun getDownloader(
        urls: List<String>,
        savePaths: List<String>,
        threadCount: Int = 64,
        chunkSizeMB: Int = 10,
        callbackUrl: String = "",
        useSocket: Boolean = false,
        showNames: List<String>? = null,
        ids: List<String>? = null
    ): Int {
        val tasksJson = buildTasksJson(urls, savePaths, showNames, ids)
        val useCallbackUrl = callbackUrl.isNotEmpty()

        val id = TTHSDLibraryJNI.getDownloader(
            tasksJson, threadCount, chunkSizeMB,
            useCallbackUrl, callbackUrl, useSocket
        )
        if (id == -1) error("[TTHSD] getDownloader 失败（JNI 返回 -1）")
        activeIds += id
        return id
    }

    fun startDownloadById(downloaderId: Int): Boolean =
        TTHSDLibraryJNI.startDownloadById(downloaderId) == 0

    fun startMultipleDownloadsById(downloaderId: Int): Boolean =
        TTHSDLibraryJNI.startMultipleDownloadsById(downloaderId) == 0

    fun pauseDownload(downloaderId: Int): Boolean =
        TTHSDLibraryJNI.pauseDownload(downloaderId) == 0

    fun resumeDownload(downloaderId: Int): Boolean =
        TTHSDLibraryJNI.resumeDownload(downloaderId) == 0

    fun stopDownload(downloaderId: Int): Boolean {
        activeIds.remove(downloaderId)
        return TTHSDLibraryJNI.stopDownload(downloaderId) == 0
    }

    override fun close() {
        activeIds.toList().forEach { stopDownload(it) }
        activeIds.clear()
    }
}
