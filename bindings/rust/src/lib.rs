//! tthsd - TTHSD 高速下载器 Rust 动态库绑定
//!
//! 在运行时通过 `libloading` 动态加载 TTHSD 动态库（.dll/.so/.dylib），
//! 提供安全的 Rust 封装 API 和基于 `tokio::sync::mpsc` 的异步事件流。
//!
//! # 快速开始
//!
//! ```rust,no_run
//! use tthsd::{TTHSDownloader, Event};
//!
//! #[tokio::main]
//! async fn main() {
//!     let mut dl = TTHSDownloader::load(None).expect("加载失败");
//!     let (id, mut rx) = dl.start_download(
//!         vec!["https://example.com/a.zip".into()],
//!         vec!["/tmp/a.zip".into()],
//!         Default::default(),
//!     ).expect("启动下载失败");
//!
//!     while let Some(evt) = rx.recv().await {
//!         match evt.event_type.as_str() {
//!             "update"  => println!("进度: {:?}", evt.data),
//!             "end"     => { println!("下载完成"); break; }
//!             "err"     => { eprintln!("错误: {:?}", evt.data); break; }
//!             _         => {}
//!         }
//!     }
//! }
//! ```

pub mod ffi;
pub mod downloader;
pub mod event;

pub use downloader::{TTHSDownloader, DownloadOptions};
pub use event::{DownloadEventMsg};
