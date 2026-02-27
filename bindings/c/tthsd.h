/**
 * tthsd.h - TTHSD 高速下载器 C/C++ 头文件
 *
 * 适用于所有支持 C ABI 的语言（C、C++、Zig、D 等）。
 * 通过 dlopen/LoadLibrary 动态加载或直接链接 TTHSD 动态库使用。
 *
 * 文档: http://p.ceroxe.fun:58000/TTHSD/
 */

#ifndef TTHSD_H
#define TTHSD_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 回调函数签名：
 *   event_json  - 事件元数据 JSON（Type/Name/ShowName/ID 字段）
 *   data_json   - 附带数据 JSON（Downloaded/Total/URL/Error 等）
 */
typedef void (*TTHSD_Callback)(const char* event_json, const char* data_json);

/**
 * start_download - 创建并立即启动下载器
 *
 * @param tasks_data        任务列表 JSON 字符串
 * @param task_count        任务数量
 * @param thread_count      下载线程数
 * @param chunk_size_mb     分块大小（MB）
 * @param callback          回调函数指针（可为 NULL）
 * @param use_callback_url  是否启用远程回调
 * @param user_agent        自定义 UA（可为 NULL）
 * @param remote_callback_url 远程回调 URL（可为 NULL）
 * @param use_socket        是否使用 Socket（bool*，可为 NULL）
 * @param is_multiple       是否并行多任务（bool*，可为 NULL）
 * @return 下载器 ID（正整数），-1 表示失败
 */
int start_download(
    const char*     tasks_data,
    int             task_count,
    int             thread_count,
    int             chunk_size_mb,
    TTHSD_Callback  callback,
    bool            use_callback_url,
    const char*     user_agent,
    const char*     remote_callback_url,
    const bool*     use_socket,
    const bool*     is_multiple
);

/**
 * get_downloader - 创建下载器实例（不立即启动）
 *
 * @return 下载器 ID，-1 表示失败
 */
int get_downloader(
    const char*     tasks_data,
    int             task_count,
    int             thread_count,
    int             chunk_size_mb,
    TTHSD_Callback  callback,
    bool            use_callback_url,
    const char*     user_agent,
    const char*     remote_callback_url,
    const bool*     use_socket
);

/** 按 ID 顺序启动下载，0=成功，-1=失败 */
int start_download_id(int id);

/** 按 ID 并行启动下载，0=成功，-1=失败 */
int start_multiple_downloads_id(int id);

/** 暂停下载 */
int pause_download(int id);

/** 恢复下载（需核心版本 >=0.5.1）*/
int resume_download(int id);

/** 停止并销毁下载器 */
int stop_download(int id);

#ifdef __cplusplus
} // extern "C"
#endif

#endif /* TTHSD_H */
