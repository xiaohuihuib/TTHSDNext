/**
 * tthsd - TTHSD 高速下载器 Node.js/TypeScript 封装包
 *
 * 主入口，导出所有公开 API。
 *
 * 快捷用法:
 * ```ts
 * import { quickDownload, EventLogger } from "tthsd";
 *
 * const id = quickDownload({
 *   urls: ["https://example.com/a.zip"],
 *   savePaths: ["./a.zip"],
 *   callback: new EventLogger(),
 * });
 * ```
 */

export * from "./types";
export { TTHSDownloader } from "./downloader";
export { resolveDllPath } from "./native";

import { TTHSDownloader } from "./downloader";
import { DownloadOptions, DownloadEvent, CallbackData } from "./types";

// ------------------------------------------------------------------
// EventLogger —— 开箱即用的控制台事件输出
// ------------------------------------------------------------------

/**
 * 内置的事件打印回调，可直接作为 callback 参数传入。
 *
 * 用法:
 * ```ts
 * import { EventLogger } from "tthsd";
 * dl.startDownload(urls, savePaths, { callback: new EventLogger() });
 * ```
 */
export class EventLogger {
  call(event: DownloadEvent, data: CallbackData): void {
    const prefix =
      event.ShowName || event.ID
        ? `[${event.ShowName}(${event.ID})]`
        : "";

    switch (event.Type) {
      case "update": {
        const d = data as { Downloaded?: number; Total?: number };
        if (d.Total && d.Total > 0) {
          const pct = ((d.Downloaded ?? 0) / d.Total) * 100;
          process.stdout.write(
            `\r${prefix} 进度: ${d.Downloaded}/${d.Total} (${pct.toFixed(2)}%)`
          );
        }
        break;
      }
      case "startOne": {
        const d = data as { URL?: string; Index?: number; Total?: number };
        console.log(`\n${prefix} ▶ 开始下载 [${d.Index}/${d.Total}]: ${d.URL}`);
        break;
      }
      case "start":
        console.log(`\n${prefix} 🚀 下载会话开始`);
        break;
      case "endOne": {
        const d = data as { URL?: string; Index?: number; Total?: number };
        console.log(`\n${prefix} ✅ 下载完成 [${d.Index}/${d.Total}]: ${d.URL}`);
        break;
      }
      case "end":
        console.log(`\n${prefix} 🏁 全部下载完成`);
        break;
      case "msg": {
        const d = data as { Text?: string };
        console.log(`\n${prefix} 📢 消息: ${d.Text}`);
        break;
      }
      case "err": {
        const d = data as { Error?: string };
        console.error(`\n${prefix} ❌ 错误: ${d.Error}`);
        break;
      }
      default:
        console.log(`\n${prefix} [未知事件 ${event.Type}]`, data);
    }
  }

  /** 让实例可直接作为回调函数传入 */
  get callback() {
    return this.call.bind(this);
  }
}

// ------------------------------------------------------------------
// quickDownload —— 一行发起下载
// ------------------------------------------------------------------

export interface QuickDownloadOptions extends DownloadOptions {
  /** 动态库路径（不填则自动搜索） */
  dllPath?: string;
}

/**
 * 快捷函数：一行代码启动下载，返回下载器 ID。
 *
 * **注意**：不等待下载完成，通过 callback 中的 `end` 事件判断完成时机。
 *
 * ```ts
 * import { quickDownload, EventLogger } from "tthsd";
 * const logger = new EventLogger();
 * const id = quickDownload({
 *   urls: ["https://cdn.example.com/file.zip"],
 *   savePaths: ["/tmp/file.zip"],
 *   callback: logger.callback,
 *   threadCount: 32,
 * });
 * ```
 */
export function quickDownload(
  options: QuickDownloadOptions & {
    urls: string[];
    savePaths: string[];
  }
): number {
  const { urls, savePaths, dllPath, ...rest } = options;
  const dl = new TTHSDownloader({ dllPath });
  return dl.startDownload(urls, savePaths, rest);
}
