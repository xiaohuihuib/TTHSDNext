package com.tthsd

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * TTHSDLibraryJNA
 *
 * JNA 接口声明，对应 TTHSD 动态库（export.rs）导出的 C ABI 函数。
 * 适用于桌面（Windows/Linux/macOS），以及 Minecraft 场景（JVM 环境下均可使用）。
 *
 * Android 端请使用 TTHSDLibraryJNI（基于 android_export.rs 的 JNI 接口）。
 */
interface TTHSDLibraryJNA : Library {

    /**
     * C 回调函数类型：void callback(const char* event_json, const char* data_json)
     */
    interface ProgressCallback : Callback {
        fun invoke(eventJson: String?, dataJson: String?)
    }

    /**
     * 创建下载器实例（不立即启动）。
     * @return 下载器 ID，失败返回 -1
     */
    fun get_downloader(
        tasksData: String,
        taskCount: Int,
        threadCount: Int,
        chunkSizeMB: Int,
        callback: ProgressCallback?,
        useCallbackUrl: Boolean,
        userAgent: String?,
        remoteCallbackUrl: String?,
        useSocket: Pointer?      // bool*，传 null 表示不启用
    ): Int

    /**
     * 创建并立即启动下载器。
     * @return 下载器 ID，失败返回 -1
     */
    fun start_download(
        tasksData: String,
        taskCount: Int,
        threadCount: Int,
        chunkSizeMB: Int,
        callback: ProgressCallback?,
        useCallbackUrl: Boolean,
        userAgent: String?,
        remoteCallbackUrl: String?,
        useSocket: Pointer?,     // bool*
        isMultiple: Pointer?     // bool*
    ): Int

    /** 按 ID 顺序启动下载 */
    fun start_download_id(id: Int): Int

    /** 按 ID 并行启动下载 */
    fun start_multiple_downloads_id(id: Int): Int

    /** 暂停 */
    fun pause_download(id: Int): Int

    /** 恢复（需核心版本 ≥0.5.1） */
    fun resume_download(id: Int): Int

    /** 停止并销毁 */
    fun stop_download(id: Int): Int

    companion object {
        /**
         * 加载 TTHSD 动态库实例（JNA 方式）。
         *
         * @param libPath 动态库绝对路径（null 则使用默认搜索逻辑）
         */
        fun load(libPath: String? = null): TTHSDLibraryJNA {
            val resolvedPath = libPath ?: NativeLibraryLoader.resolve()
            return Native.load(resolvedPath, TTHSDLibraryJNA::class.java)
        }
    }
}
