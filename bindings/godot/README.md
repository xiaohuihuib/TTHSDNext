# TTHSD Godot GDExtension 插件

> 基于 Rust 实现的高性能下载器在 Godot 4 中的 GDExtension 封装，支持多线程分块下载，通过 Godot Signal 实时推送下载进度。

## 编译

### 前置要求
- CMake >= 3.22
- C++17 编译器（MSVC / GCC / Clang）
- [godot-cpp](https://github.com/godotengine/godot-cpp)（以 git submodule 形式放置到 `thirdparty/godot-cpp`）
- [nlohmann/json](https://github.com/nlohmann/json)（header-only，放置到 `thirdparty/json/include`）
- TTHSD 动态库（`.dll`/`.so`/`.dylib`）已编译

### 构建步骤

```bash
mkdir build && cd build

# Linux / macOS
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)

# Windows (Visual Studio)
cmake .. -G "Visual Studio 17 2022" -A x64
cmake --build . --config Release
```

构建产物会自动放入 `project/addons/tthsd/bin/`。

### 将 TTHSD 动态库放入 addons 目录

```
project/addons/tthsd/bin/
├── libtthsd_godot.so    # Linux GDExtension 插件
├── tthsd_godot.dll      # Windows GDExtension 插件
├── TTHSD.so             # Linux TTHSD 核心库
├── TTHSD.dll            # Windows TTHSD 核心库
└── TTHSD.dylib          # macOS TTHSD 核心库
```

## 在 Godot 中使用

1. 将 `project/addons/tthsd/` 目录复制到你的 Godot 项目的 `addons/` 目录
2. 项目设置 → 插件 → 启用 **tthsd**
3. 编写 GDScript：

```gdscript
extends Node

var downloader: TTHSDownloader

func _ready():
    downloader = TTHSDownloader.new()

    # 加载 TTHSD 动态库（传入绝对路径，或留空自动搜索）
    downloader.load_library("")

    # 连接信号
    downloader.on_progress.connect(_on_progress)
    downloader.on_finished.connect(_on_finished)
    downloader.on_error.connect(_on_error)

    # 启动下载
    var id = downloader.start_download(
        ["https://example.com/a.zip"],
        ["/tmp/a.zip"],
        64,   # thread_count
        10    # chunk_size_mb
    )

func _on_progress(event: Dictionary, data: Dictionary):
    var pct = float(data["Downloaded"]) / float(data["Total"]) * 100.0
    print("进度: %.2f%%" % pct)

func _on_finished(event: Dictionary, data: Dictionary):
    print("✅ 完成: " + event.get("ShowName", ""))

func _on_error(event: Dictionary, data: Dictionary):
    push_error("❌ " + data.get("Error", ""))
```

## 可用信号

| 信号 | 触发时机 | data 包含字段 |
|------|---------|---------------|
| `on_progress(event, data)` | 每 512KB 进度更新 | `Downloaded`, `Total` |
| `on_finished(event, data)` | 单个 / 全部任务结束 | `URL`, `Index`, `Total` |
| `on_error(event, data)` | 下载出错 | `Error` |
| `on_event(event, data)` | 其余所有事件 | 根据 `event.Type` 不同而异 |

## 可用方法

| 方法 | 说明 |
|------|------|
| `load_library(path: String) -> bool` | 加载动态库（必须首先调用） |
| `start_download(urls, paths, threads, chunk) -> int` | 创建并立即启动下载 |
| `get_downloader(urls, paths, threads, chunk) -> int` | 创建但不启动 |
| `start_download_by_id(id) -> bool` | 按 ID 顺序启动 |
| `start_multiple_downloads_by_id(id) -> bool` | 按 ID 并行启动 |
| `pause_download(id) -> bool` | 暂停 |
| `resume_download(id) -> bool` | 恢复 |
| `stop_download(id) -> bool` | 停止并销毁 |
