#pragma once

#include <godot_cpp/classes/ref_counted.hpp>
#include <godot_cpp/core/class_db.hpp>
#include <godot_cpp/variant/dictionary.hpp>
#include <godot_cpp/variant/string.hpp>
#include <godot_cpp/variant/typed_array.hpp>

#include "tthsd_api.h"

namespace godot {

/**
 * TTHSDownloader - Godot GDExtension 封装节点
 *
 * 在 GDScript 中使用：
 * ```gdscript
 * var dl = TTHSDownloader.new()
 * dl.on_progress.connect(_on_progress)
 * dl.on_error.connect(_on_error)
 * dl.on_finished.connect(_on_finished)
 * var id = dl.start_download(
 *     ["https://example.com/a.zip"],
 *     ["/tmp/a.zip"],
 *     64, 10
 * )
 * ```
 */
class TTHSDownloader : public RefCounted {
    GDCLASS(TTHSDownloader, RefCounted)

public:
    TTHSDownloader();
    ~TTHSDownloader();

    /**
     * 加载 TTHSD 动态库（必须在使用其他方法之前调用）
     * @param lib_path 动态库路径（留空则自动搜索）
     * @return 成功返回 true
     */
    bool load_library(const String& lib_path = "");

    /**
     * 创建下载器并立即启动（返回下载器 ID）
     */
    int start_download(
        TypedArray<String> urls,
        TypedArray<String> save_paths,
        int thread_count = 64,
        int chunk_size_mb = 10
    );

    /**
     * 创建下载器但不立即启动（返回下载器 ID）
     */
    int get_downloader(
        TypedArray<String> urls,
        TypedArray<String> save_paths,
        int thread_count = 64,
        int chunk_size_mb = 10
    );

    bool start_download_by_id(int id);
    bool start_multiple_downloads_by_id(int id);
    bool pause_download(int id);
    bool resume_download(int id);
    bool stop_download(int id);

protected:
    static void _bind_methods();

private:
    // 动态库句柄
    void* _lib_handle = nullptr;

    // 函数指针
    FnGetDownloader           _fn_get_downloader           = nullptr;
    FnStartDownload           _fn_start_download           = nullptr;
    FnStartDownloadId         _fn_start_download_id        = nullptr;
    FnStartMultipleDownloadsId _fn_start_multiple         = nullptr;
    FnPauseDownload           _fn_pause_download           = nullptr;
    FnResumeDownload          _fn_resume_download          = nullptr;
    FnStopDownload            _fn_stop_download            = nullptr;

    bool _loaded = false;

    // 静态 C 回调（从 C 回调路由到 Godot Signal）
    static void _c_callback(const char* event_json, const char* data_json);
    // 每个下载器 ID 对应的 GDScript 回调注册
    static TTHSDownloader* _instance;  // 简化版：单例转发

    String _build_tasks_json(
        const TypedArray<String>& urls,
        const TypedArray<String>& save_paths
    );
};

} // namespace godot
