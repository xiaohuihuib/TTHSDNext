//! 安全的 Rust 封装层：TTHSDownloader
//!
//! 将底层的 C FFI 调用包装为 Rust 惯用的安全 API，
//! 并通过 `tokio::sync::mpsc` channel 提供异步事件流。

use std::ffi::{CString, c_void};
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use tokio::sync::mpsc;
use uuid::Uuid;

use crate::ffi::TthsdRaw;
use crate::event::{DownloadEvent, DownloadEventMsg, EventData};

/// 任务描述
#[derive(Debug, Clone)]
pub struct DownloadTask {
    pub url: String,
    pub save_path: String,
    pub show_name: String,
    pub id: String,
}

/// start_download / get_downloader 的可选参数
#[derive(Debug, Clone, Default)]
pub struct DownloadOptions {
    pub thread_count: Option<usize>,
    pub chunk_size_mb: Option<usize>,
    pub user_agent: Option<String>,
    pub use_callback_url: bool,
    pub remote_callback_url: Option<String>,
    pub use_socket: Option<bool>,
    pub is_multiple: Option<bool>,
}

// ------------------------------------------------------------------
// 全局回调表：将 downloader_id 映射到 mpsc::Sender
// 因为 C 回调没有 userdata 指针，所以用全局表绕过此限制
// ------------------------------------------------------------------
type SenderMap = HashMap<i32, mpsc::UnboundedSender<DownloadEventMsg>>;

static SENDER_MAP: std::sync::OnceLock<Mutex<SenderMap>> = std::sync::OnceLock::new();

fn sender_map() -> &'static Mutex<SenderMap> {
    SENDER_MAP.get_or_init(|| Mutex::new(HashMap::new()))
}

/// 注册一个 id → sender 映射，并返回对应的 receiver
fn register_channel(id: i32) -> mpsc::UnboundedReceiver<DownloadEventMsg> {
    let (tx, rx) = mpsc::unbounded_channel();
    sender_map().lock().unwrap().insert(id, tx);
    rx
}

fn unregister_channel(id: i32) {
    sender_map().lock().unwrap().remove(&id);
}

/// TTHSD C 回调 → Rust 的静态转发函数
///
/// 因为 C 回调不携带 userdata，所以通过全局 sender_map 广播给所有 channel。
/// 实际工程中回调携带 ID 字段，通过 event.ID 做精确路由。  
extern "C" fn global_c_callback(
    event_ptr: *const std::ffi::c_char,
    data_ptr: *const std::ffi::c_char,
) {
    // Safety: DLL 保证传入有效 C 字符串
    let event_str = unsafe {
        if event_ptr.is_null() { return; }
        std::ffi::CStr::from_ptr(event_ptr).to_str().unwrap_or("{}")
    };
    let data_str = unsafe {
        if data_ptr.is_null() { "{}" } else {
            std::ffi::CStr::from_ptr(data_ptr).to_str().unwrap_or("{}")
        }
    };

    let event: DownloadEvent = match serde_json::from_str(event_str) {
        Ok(e) => e,
        Err(_) => return,
    };
    let data: EventData = serde_json::from_str(data_str).unwrap_or_default();

    let msg = DownloadEventMsg { event: event.clone(), data };

    // 广播到所有注册的 channel（通常同一时刻只有少量 channel）
    let map = sender_map().lock().unwrap();
    for sender in map.values() {
        let _ = sender.send(msg.clone());
    }
}

// ------------------------------------------------------------------
// TTHSDownloader
// ------------------------------------------------------------------

/// TTHSD 高速下载器安全 Rust 封装
///
/// 通过 `libloading` 动态加载 TTHSD 动态库（.dll/.so/.dylib），
/// 提供安全 API 并返回异步 `mpsc::UnboundedReceiver<DownloadEventMsg>` 事件流。
pub struct TTHSDownloader {
    raw: Arc<TthsdRaw>,
}

impl TTHSDownloader {
    /// 加载 TTHSD 动态库
    ///
    /// @param lib_path 动态库路径（`None` 则在当前目录搜索默认名称）
    pub fn load(lib_path: Option<&Path>) -> Result<Self, Box<dyn std::error::Error>> {
        let path: PathBuf = match lib_path {
            Some(p) => p.to_path_buf(),
            None => PathBuf::from(TthsdRaw::default_lib_name()),
        };
        let raw = TthsdRaw::load(&path)?;
        Ok(Self { raw: Arc::new(raw) })
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    fn build_tasks_json(
        urls: &[String],
        save_paths: &[String],
        show_names: Option<&[String]>,
        ids: Option<&[String]>,
    ) -> Result<CString, Box<dyn std::error::Error>> {
        assert_eq!(urls.len(), save_paths.len(), "urls 与 save_paths 长度不一致");
        let tasks: Vec<serde_json::Value> = urls.iter().enumerate().map(|(i, url)| {
            let show_name = show_names
                .and_then(|s| s.get(i))
                .map(|s| s.as_str())
                .unwrap_or_else(|| {
                    url.rsplit('/').next().unwrap_or("").split('?').next().unwrap_or("")
                });
            let id = ids
                .and_then(|s| s.get(i))
                .map(|s| s.clone())
                .unwrap_or_else(|| Uuid::new_v4().to_string());
            serde_json::json!({
                "url":       url,
                "save_path": save_paths[i],
                "show_name": if show_name.is_empty() { format!("task_{}", i) } else { show_name.to_string() },
                "id":        id,
            })
        }).collect();
        Ok(CString::new(serde_json::to_string(&tasks)?)?)
    }

    // ------------------------------------------------------------------
    // 公开 API
    // ------------------------------------------------------------------

    /// 创建下载器并**立即启动**，返回 `(下载器 ID, 异步事件 Receiver)`
    pub fn start_download(
        &self,
        urls: Vec<String>,
        save_paths: Vec<String>,
        opts: DownloadOptions,
    ) -> Result<(i32, mpsc::UnboundedReceiver<DownloadEventMsg>), Box<dyn std::error::Error>> {
        let tasks_json = Self::build_tasks_json(&urls, &save_paths, None, None)?;
        let thread_count = opts.thread_count.unwrap_or(64) as i32;
        let chunk_size_mb = opts.chunk_size_mb.unwrap_or(10) as i32;
        let ua = opts.user_agent.as_deref().map(CString::new).transpose()?;
        let cb_url = opts.remote_callback_url.as_deref().map(CString::new).transpose()?;

        let use_socket_val: Option<bool> = opts.use_socket;
        let is_multiple_val: Option<bool> = opts.is_multiple;

        let id = unsafe {
            (self.raw.fn_start_download)(
                tasks_json.as_ptr(),
                urls.len() as i32,
                thread_count,
                chunk_size_mb,
                global_c_callback as *mut c_void,
                opts.use_callback_url,
                ua.as_ref().map_or(std::ptr::null(), |s| s.as_ptr()),
                cb_url.as_ref().map_or(std::ptr::null(), |s| s.as_ptr()),
                use_socket_val.as_ref().map_or(std::ptr::null(), |v| v as *const bool),
                is_multiple_val.as_ref().map_or(std::ptr::null(), |v| v as *const bool),
            )
        };

        if id == -1 {
            return Err("start_download failed: DLL returned -1".into());
        }

        let rx = register_channel(id);
        Ok((id, rx))
    }

    /// 创建下载器**但不启动**，返回 `(下载器 ID, 异步事件 Receiver)`
    pub fn get_downloader(
        &self,
        urls: Vec<String>,
        save_paths: Vec<String>,
        opts: DownloadOptions,
    ) -> Result<(i32, mpsc::UnboundedReceiver<DownloadEventMsg>), Box<dyn std::error::Error>> {
        let tasks_json = Self::build_tasks_json(&urls, &save_paths, None, None)?;
        let thread_count = opts.thread_count.unwrap_or(64) as i32;
        let chunk_size_mb = opts.chunk_size_mb.unwrap_or(10) as i32;
        let ua = opts.user_agent.as_deref().map(CString::new).transpose()?;
        let cb_url = opts.remote_callback_url.as_deref().map(CString::new).transpose()?;
        let use_socket_val = opts.use_socket;

        let id = unsafe {
            (self.raw.fn_get_downloader)(
                tasks_json.as_ptr(),
                urls.len() as i32,
                thread_count,
                chunk_size_mb,
                global_c_callback as *mut c_void,
                opts.use_callback_url,
                ua.as_ref().map_or(std::ptr::null(), |s| s.as_ptr()),
                cb_url.as_ref().map_or(std::ptr::null(), |s| s.as_ptr()),
                use_socket_val.as_ref().map_or(std::ptr::null(), |v| v as *const bool),
            )
        };

        if id == -1 {
            return Err("get_downloader failed: DLL returned -1".into());
        }

        let rx = register_channel(id);
        Ok((id, rx))
    }

    pub fn start_download_by_id(&self, id: i32) -> bool {
        unsafe { (self.raw.fn_start_download_id)(id) == 0 }
    }

    pub fn start_multiple_downloads_by_id(&self, id: i32) -> bool {
        unsafe { (self.raw.fn_start_multiple_downloads_id)(id) == 0 }
    }

    pub fn pause_download(&self, id: i32) -> bool {
        unsafe { (self.raw.fn_pause_download)(id) == 0 }
    }

    pub fn resume_download(&self, id: i32) -> bool {
        unsafe { (self.raw.fn_resume_download)(id) == 0 }
    }

    pub fn stop_download(&self, id: i32) -> bool {
        let ret = unsafe { (self.raw.fn_stop_download)(id) == 0 };
        unregister_channel(id);
        ret
    }
}
