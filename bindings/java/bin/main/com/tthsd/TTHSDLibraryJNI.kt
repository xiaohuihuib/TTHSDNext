package com.tthsd

/**
 * TTHSDLibraryJNI - Android 平台 JNI 接口
 *
 * 本类在启用 Android 特性时（即编译了 android_export.rs 的 JNI 导出），
 * 通过 System.loadLibrary("TTHSD") 加载 .so 文件并直接调用 JNI 方法。
 *
 * 对应的 Rust 实现位于 src/core/android_export.rs 中
 * Java 类名: com.tthsd.TTHSDLibrary
 */
object TTHSDLibraryJNI {

    private var _loaded = false

    /**
     * 加载 Android 动态库（须在使用前调用，通常在 Application.onCreate() 中）
     */
    fun load() {
        if (_loaded) return
        System.loadLibrary("TTHSD")
        _loaded = true
    }

    // ------------------------------------------------------------------
    // JNI 原生方法声明（对应 android_export.rs 的导出函数）
    // ------------------------------------------------------------------

    /**
     * 创建并立即启动下载
     * @param tasksJson        任务列表 JSON 字符串
     * @param threadCount      下载线程数
     * @param chunkSizeMB      分块大小（MB）
     * @param useCallbackUrl   是否启用远程回调 URL
     * @param callbackUrl      远程回调 URL（WebSocket 或 Socket）
     * @param useSocket        是否使用 TCP Socket（否则用 WebSocket）
     * @param isMultiple       是否并行多任务下载
     * @return 下载器 ID，失败返回 -1
     */
    @JvmStatic
    external fun startDownload(
        tasksJson: String,
        threadCount: Int,
        chunkSizeMB: Int,
        useCallbackUrl: Boolean,
        callbackUrl: String,
        useSocket: Boolean,
        isMultiple: Boolean
    ): Int

    /**
     * 创建下载器（不立即启动）
     * @return 下载器 ID，失败返回 -1
     */
    @JvmStatic
    external fun getDownloader(
        tasksJson: String,
        threadCount: Int,
        chunkSizeMB: Int,
        useCallbackUrl: Boolean,
        callbackUrl: String,
        useSocket: Boolean
    ): Int

    @JvmStatic external fun startDownloadById(id: Int): Int
    @JvmStatic external fun startMultipleDownloadsById(id: Int): Int
    @JvmStatic external fun pauseDownload(id: Int): Int
    @JvmStatic external fun resumeDownload(id: Int): Int
    @JvmStatic external fun stopDownload(id: Int): Int
}
