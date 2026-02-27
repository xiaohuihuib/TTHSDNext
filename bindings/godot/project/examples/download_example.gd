extends Node

## TTHSD 下载器 GDScript 示例
## 将本脚本挂载到任意节点，运行场景后即可看到下载进度输出。

@onready var downloader: TTHSDownloader = TTHSDownloader.new()

func _ready() -> void:
    # 1. 加载 TTHSD 动态库（留空则自动搜索同目录）
    if not downloader.load_library(""):
        push_error("[TTHSD] 动态库加载失败")
        return

    # 2. 连接信号
    downloader.on_progress.connect(_on_progress)
    downloader.on_finished.connect(_on_finished)
    downloader.on_error.connect(_on_error)
    downloader.on_event.connect(_on_event)

    # 3. 启动下载（返回下载器 ID）
    var id: int = downloader.start_download(
        ["https://example.com/a.zip",
         "https://example.com/b.zip"],
        ["/tmp/a.zip",
         "/tmp/b.zip"],
        64,   # thread_count
        10    # chunk_size_mb
    )

    if id == -1:
        push_error("[TTHSD] start_download 失败")
        return

    print("[TTHSD] 下载器已启动，ID = %d" % id)

    # 可选：稍后暂停 / 恢复 / 停止
    # await get_tree().create_timer(3.0).timeout
    # downloader.pause_download(id)
    # await get_tree().create_timer(2.0).timeout
    # downloader.resume_download(id)


## 进度更新回调（高频，每 512KB 触发一次）
func _on_progress(event: Dictionary, data: Dictionary) -> void:
    var downloaded: int = data.get("Downloaded", 0)
    var total: int      = data.get("Total", 1)
    var pct: float      = float(downloaded) / float(total) * 100.0
    print("[%s] 进度: %d/%d (%.2f%%)" % [event.get("ShowName", ""), downloaded, total, pct])


## 单个 / 全部任务完成回调
func _on_finished(event: Dictionary, data: Dictionary) -> void:
    var event_type: String = event.get("Type", "")
    if event_type == "endOne":
        print("✅ 完成 [%d/%d]: %s" % [
            data.get("Index", 0),
            data.get("Total", 0),
            data.get("URL", "")
        ])
    elif event_type == "end":
        print("🏁 全部下载完成")


## 错误回调
func _on_error(event: Dictionary, data: Dictionary) -> void:
    push_error("❌ [TTHSD] 下载错误: %s (%s)" % [
        data.get("Error", "未知错误"),
        event.get("ShowName", "")
    ])


## 其他事件（start / startOne / msg 等）
func _on_event(event: Dictionary, _data: Dictionary) -> void:
    var t: String = event.get("Type", "")
    if t == "start":
        print("🚀 下载会话开始")
    elif t == "startOne":
        print("▶ 开始下载: %s [%d/%d]" % [
            event.get("ShowName", ""),
            _data.get("Index", 0),
            _data.get("Total", 0)
        ])
    elif t == "msg":
        print("📢 消息: %s" % _data.get("Text", ""))
