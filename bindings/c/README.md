# TTHSD C / C++ / C# 封装

> 为 C、C++ 和 C#（.NET）提供的 TTHSD 高速下载器接口封装。

## 文件说明

| 文件 | 用途 |
|------|------|
| `tthsd.h` | 标准 C 头文件——声明所有 C ABI 导出函数及回调类型 |
| `TTHSDownloader.hpp` | C++ header-only 封装类——RAII 持有库句柄，`std::function` 回调 |
| `../csharp/TTHSDownloader.cs` | C# P/Invoke 封装——`async/await` 事件流，支持 WPF / AvaloniaUI / Unity |

---

## C 用法（直接调用 `tthsd.h`）

```c
#include "tthsd.h"

void my_callback(const char* event_json, const char* data_json) {
    // 自行解析 JSON
    printf("event: %s\n", event_json);
}

int main() {
    const char* tasks = "[{\"url\":\"https://example.com/a.zip\","
                         "\"save_path\":\"/tmp/a.zip\","
                         "\"show_name\":\"a.zip\","
                         "\"id\":\"1\"}]";
    int id = start_download(tasks, 1, 32, 10, my_callback,
                            false, NULL, NULL, NULL, NULL);
    // ... 等待完成 ...
    stop_download(id);
    return 0;
}
```

编译（Linux）：

```bash
gcc main.c -L. -lTTHSD -ldl -o my_app
# 或通过 dlopen 手动加载，则无需 -L/-lTTHSD
```

---

## C++ 用法（`TTHSDownloader.hpp`）

```cpp
#include "TTHSDownloader.hpp"

int main() {
    TTHSDownloader dl;
    dl.load();  // 自动搜索 TTHSD.dll/so/dylib

    int id = dl.startDownload(
        {"https://example.com/a.zip"},
        {"/tmp/a.zip"},
        DownloadParams{.threadCount = 32},
        [](const json& event, const json& data) {
            if (event["Type"] == "update")
                printf("\r进度: %.2f%%",
                    data["Downloaded"].get<double>() /
                    data["Total"].get<double>() * 100.0);
        }
    );
    // ...
    dl.stopDownload(id);
}
```

编译示例（使用 CMake 示例工程）：

```bash
cd c/example
mkdir build && cd build
cmake .. && make -j$(nproc)
cp /path/to/TTHSD.so ./
./download_example
```

---

## C# 用法（`TTHSDownloader.cs`）

```csharp
using TTHSD;

await using var dl = new TTHSDownloader();

var (id, events) = dl.StartDownload(
    new[] { "https://example.com/a.zip" },
    new[] { "/tmp/a.zip" },
    threadCount: 32
);

await foreach (var ev in events)
{
    if (ev.Event.Type == "update")
        Console.Write($"\r进度: {ev.Data["Downloaded"]}/{ev.Data["Total"]}");
    else if (ev.Event.Type == "end")
    {
        Console.WriteLine("\n完成");
        break;
    }
}
```

运行示例：

```bash
cd csharp/example
dotnet run
```

> 确保 `TTHSD.dll`（Windows）/ `TTHSD.so`（Linux）/ `TTHSD.dylib`（macOS）位于工作目录或系统库路径。

---

## Unity 集成

1. 将 `TTHSDownloader.cs` 放入 `Assets/Plugins/TTHSD/`
2. 将对应平台的 `TTHSD.dll`/`TTHSD.so` 放入 `Assets/Plugins/<平台>/`
3. 在 Unity Editor → Plugin Inspector 中设置正确平台和 CPU 架构
4. 代码中正常 `using TTHSD;` 调用即可（注意回调必须切换回主线程：使用 `UnityMainThreadDispatcher` 或 `SynchronizationContext`）
