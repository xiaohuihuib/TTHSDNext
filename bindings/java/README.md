# TTHSD Java/Kotlin 封装库

> 基于 JNA（桌面端）和 JNI（Android 端）调用 TTHSD 高速下载器，可用于 Windows / Linux / macOS 桌面程序，以及 Android 应用、Minecraft Mod/Plugin、第三方启动器等场景。

## 安装

将以下依赖添加到你的 `build.gradle.kts`（或上游 Maven）：

```kotlin
// 如果通过本地 jar 引用：
implementation(files("libs/tthsd-0.5.1.jar"))

// 若发布到 Maven Central：
// implementation("com.tthsd:tthsd:0.5.1")

// 必需依赖（JNA + Gson）
implementation("net.java.dev.jna:jna:5.15.0")
implementation("com.google.code.gson:gson:2.11.0")
```

## 桌面端（Windows / Linux / macOS）用法

```kotlin
import com.tthsd.TTHSDownloader
import com.tthsd.DownloadEvent

// TTHSDownloader 实现了 AutoCloseable，推荐用 use {}
TTHSDownloader().use { dl ->
    val id = dl.startDownload(
        urls = listOf("https://example.com/a.zip", "https://example.com/b.zip"),
        savePaths = listOf("/tmp/a.zip", "/tmp/b.zip"),
        threadCount = 64,
        chunkSizeMB = 10,
        callback = { event: DownloadEvent, data: Map<String, Any?> ->
            when (event.Type) {
                "update"  -> println("进度: ${data["Downloaded"]}/${data["Total"]}")
                "end"     -> println("✅ 全部完成")
                "err"     -> System.err.println("❌ 错误: ${data["Error"]}")
            }
        }
    )
    // id 可用于暂停/恢复/停止
    // dl.pauseDownload(id)
    // dl.resumeDownload(id)
    // dl.stopDownload(id)
}
```

### 两步走：先创建，后启动

```kotlin
val dl = TTHSDownloader()
val id = dl.getDownloader(urls, savePaths)   // 创建但不启动
dl.startDownloadById(id)                     // 顺序启动
// 或
dl.startMultipleDownloadsById(id)            // 并行启动
```

### 动态库查找规则
1. `TTHSD_LIB_PATH` 环境变量
2. fat-jar 内嵌资源（`/native/<os>/<arch>/TTHSD.*`），自动提取到 `java.io.tmpdir/tthsd_native/`
3. `user.dir`（工作目录）
4. JAR 所在目录

## Android 端用法

Android 端使用 JNI 接口，通过 `soLibs/` 中的 `.so` 文件。由于 Android JNI 不支持 C 函数指针回调，进度反馈需通过 WebSocket/Socket 实现。

```kotlin
import com.tthsd.TTHSDownloaderAndroid

// 在 Application.onCreate() 或首次使用前调用
// （实际加载由 TTHSDownloaderAndroid 初始化块自动完成）

TTHSDownloaderAndroid().use { dl ->
    val id = dl.startDownload(
        urls = listOf("https://example.com/a.zip"),
        savePaths = listOf("/sdcard/Download/a.zip"),
        callbackUrl = "ws://192.168.1.100:8080",  // 可选：WebSocket 进度回调
        useSocket = false                           // false=WebSocket, true=TCP Socket
    )
}
```

## 在 Minecraft Mod / Plugin 中集成

由于本 jar 不依赖任何 MC 特有 API，可直接在 Mod/Plugin 中作为普通 Kotlin/Java 库引用：

```kotlin
// Fabric Mod 示例（forge/bukkit 同理）
class MyMod : ModInitializer {
    private val downloader = TTHSDownloader()

    override fun onInitialize() {
        // 正常调用 downloader.startDownload(...) 即可
    }
}
```

将 `tthsd-X.X.X.jar` 放入 Fabric 的 `mods/` 目录或作为 lib 打包进 Mod 的 JAR 均可正常工作。
