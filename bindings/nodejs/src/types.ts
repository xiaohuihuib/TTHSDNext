/**
 * TTHSD TypeScript 接口类型定义
 *
 * 包含所有公开 API 的类型描述，与 DLL 的 C ABI 保持一一对应。
 */

/** 下载任务描述 */
export interface DownloadTask {
  /** 待下载的 URL */
  url: string;
  /** 保存到本地的完整路径 */
  save_path: string;
  /** 在回调事件中显示的名称 */
  show_name: string;
  /** 任务唯一 ID（可自定义，不填则自动生成 UUID） */
  id: string;
}

/** 下载事件类型 */
export type EventType =
  | "start"      // 所有任务开始
  | "startOne"   // 单个任务开始
  | "update"     // 进度更新
  | "end"        // 所有任务结束
  | "endOne"     // 单个任务结束
  | "msg"        // 消息通知
  | "err";       // 错误

/** DLL 回调中 event 字段的结构 */
export interface DownloadEvent {
  Type: EventType;
  Name: string;
  ShowName: string;
  ID: string;
}

/** 进度更新 (update) 附带数据 */
export interface ProgressData {
  Downloaded: number;
  Total: number;
}

/** 任务开始/结束 附带数据 */
export interface TaskBoundaryData {
  URL: string;
  SavePath: string;
  ShowName: string;
  Index: number;
  Total: number;
}

/** 消息附带数据 */
export interface MsgData {
  Text: string;
}

/** 错误附带数据 */
export interface ErrorData {
  Error: string;
}

/** 回调数据联合类型 */
export type CallbackData =
  | ProgressData
  | TaskBoundaryData
  | MsgData
  | ErrorData
  | Record<string, unknown>;

/** 用户注册的回调函数签名 */
export type DownloadCallback = (event: DownloadEvent, data: CallbackData) => void;

/** start_download / get_downloader 的参数选项 */
export interface DownloadOptions {
  /** 下载线程数（默认 64） */
  threadCount?: number;
  /** 每个分块大小，单位 MB（默认 10） */
  chunkSizeMB?: number;
  /** 进度/事件回调 */
  callback?: DownloadCallback;
  /** 自定义 User-Agent（不填使用 DLL 内置默认值） */
  userAgent?: string;
  /** 是否启用远程回调 URL（需配合 remoteCallbackUrl 使用） */
  useCallbackUrl?: boolean;
  /** 远程回调地址（WebSocket 或 Socket） */
  remoteCallbackUrl?: string;
  /** 回调 URL 使用 Socket 而非 WebSocket */
  useSocket?: boolean;
  /** 并行多任务下载（true）还是顺序（false，默认） */
  isMultiple?: boolean;
  /** 各任务显示名称覆盖 */
  showNames?: string[];
  /** 各任务 ID 覆盖 */
  ids?: string[];
}
