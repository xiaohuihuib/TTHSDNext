# tthsd

> TTHSD 高速下载器 Node.js/TypeScript 封装（支持 Electron）

基于 [Koffi](https://koffi.dev) 动态加载 `TTHSD.dll` / `TTHSD.so` / `TTHSD.dylib`，对外提供强类型的 TypeScript API。

## 安装

```bash
npm install tthsd
# 或
yarn add tthsd
```

> **注意**：动态库文件（`TTHSD.dll`/`TTHSD.so`/`TTHSD.dylib`）需要从 [TTHSD Releases](https://github.com/your-org/TTHSD) 下载，并放置在正确的目录（详见下方）。

## 快速开始

```ts
import { quickDownload, EventLogger } from "tthsd";

const logger = new EventLogger();

quickDownload({
  urls: ["https://cdn.example.com/bigfile.zip"],
  savePaths: ["/tmp/bigfile.zip"],
  callback: logger.callback,
  threadCount: 32,
});
// 输出: 🚀 下载会话开始 → 进度: xxx/xxx (xx.xx%) → ✅ 全部下载完成
```

## 进阶用法

```ts
import { TTHSDownloader } from "tthsd";

const dl = new TTHSDownloader();

// 创建但不立即启动
const id = dl.getDownloader(
  ["https://example.com/a.zip", "https://example.com/b.zip"],
  ["/tmp/a.zip", "/tmp/b.zip"],
  { threadCount: 64, chunkSizeMB: 10 }
);

// 之后手动启动（顺序）
dl.startDownloadById(id);
// 或并行启动
// dl.startMultipleDownloadsById(id);

// 暂停 / 恢复 / 停止
dl.pauseDownload(id);
dl.resumeDownload(id);
dl.stopDownload(id);

// 程序退出前清理
dl.dispose();
```

## 回调事件

`callback(event, data)` 中的 `event.Type` 取值及对应 `data` 结构：

| `event.Type` | `data` 包含字段 |
|---|---|
| `"start"` | — |
| `"startOne"` | `URL`, `SavePath`, `ShowName`, `Index`, `Total` |
| `"update"` | `Downloaded`, `Total` |
| `"endOne"` | `URL`, `SavePath`, `ShowName`, `Index`, `Total` |
| `"end"` | — |
| `"msg"` | `Text` |
| `"err"` | `Error` |

## Electron 集成

在 Electron 打包时，需要将动态库放置在 `resources/app.asar.unpacked/` 目录。修改 `electron-builder.yml`：

```yaml
extraResources:
  - from: "native/TTHSD.dll"      # Windows
    to: "app.asar.unpacked/TTHSD.dll"
  - from: "native/TTHSD.so"       # Linux
    to: "app.asar.unpacked/TTHSD.so"
  - from: "native/TTHSD.dylib"    # macOS
    to: "app.asar.unpacked/TTHSD.dylib"
```

`tthsd` 会自动搜索 `resources/app.asar.unpacked/` 等路径，无需手动指定路径。

## 动态库搜索顺序

1. 构造函数中的 `dllPath` 参数  
2. Electron `resources/app.asar.unpacked/`  
3. `process.execPath` 同级目录  
4. `process.cwd()`  
5. `__dirname` 及上级目录
