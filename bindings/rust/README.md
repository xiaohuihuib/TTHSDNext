# tthsd (Rust)

> TTHSD 高速下载器 Rust 封装 Crate。通过 `libloading` 在运行时动态加载 TTHSD 动态库，提供安全的 Rust API 和 `tokio::sync::mpsc` 异步事件流。

## 使用

```toml
# Cargo.toml
[dependencies]
tthsd = { path = "bindings/rust" }  # 本地路径引用
# 或发布后：tthsd = "0.5.1"
tokio = { version = "1", features = ["full"] }
```

```rust
use tthsd::{TTHSDownloader, DownloadOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 加载动态库（None = 当前目录自动搜索）
    let dl = TTHSDownloader::load(None)?;

    let (id, mut rx) = dl.start_download(
        vec!["https://example.com/file.zip".to_string()],
        vec!["/tmp/file.zip".to_string()],
        DownloadOptions {
            thread_count: Some(32),
            ..Default::default()
        },
    )?;

    while let Some(msg) = rx.recv().await {
        match msg.event.event_type.as_str() {
            "update" => {
                let d = msg.data.get("Downloaded").and_then(|v| v.as_i64()).unwrap_or(0);
                let t = msg.data.get("Total").and_then(|v| v.as_i64()).unwrap_or(1);
                print!("\r进度: {:.2}%", d as f64 / t as f64 * 100.0);
            }
            "end" => { println!("\n完成"); break; }
            "err" => { eprintln!("\n错误: {:?}", msg.data); break; }
            _ => {}
        }
    }
    dl.stop_download(id);
    Ok(())
}
```

## 动态库搜索顺序

1. 传入 `TTHSDownloader::load(Some(path))`
2. 当前目录（`TTHSD.dll` / `TTHSD.so` / `TTHSD.dylib`）

## 运行示例

```bash
# 确保 TTHSD.so 在当前目录
cargo run --example basic_download
```

## 架构

| 模块 | 说明 |
|------|------|
| `ffi.rs` | `libloading` 加载动态库，持有所有 C ABI 函数指针 |
| `event.rs` | `DownloadEvent`、`EventData` 类型定义 |
| `downloader.rs` | 安全封装、全局 C 回调路由、`mpsc channel` 事件流 |
