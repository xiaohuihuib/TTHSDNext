#pragma once

#include <cstdint>

// ------------------------------------------------------------------
// TTHSD C ABI 函数声明（对应 Rust export.rs）
// 在运行时由 dlopen / LoadLibrary 动态加载
// ------------------------------------------------------------------

extern "C" {

/// 函数指针类型：回调签名 void callback(const char* event, const char* data)
using TTHSDCallback = void (*)(const char* event_json, const char* data_json);

/// 创建下载器（不立即启动）
using FnGetDownloader = int (*)(
    const char* tasks_data,
    int         task_count,
    int         thread_count,
    int         chunk_size_mb,
    void*       callback,
    bool        use_callback_url,
    const char* user_agent,
    const char* remote_callback_url,
    const bool* use_socket
);

/// 创建并立即启动下载
using FnStartDownload = int (*)(
    const char* tasks_data,
    int         task_count,
    int         thread_count,
    int         chunk_size_mb,
    void*       callback,
    bool        use_callback_url,
    const char* user_agent,
    const char* remote_callback_url,
    const bool* use_socket,
    const bool* is_multiple
);

using FnStartDownloadId           = int (*)(int id);
using FnStartMultipleDownloadsId  = int (*)(int id);
using FnPauseDownload             = int (*)(int id);
using FnResumeDownload            = int (*)(int id);
using FnStopDownload              = int (*)(int id);

} // extern "C"
