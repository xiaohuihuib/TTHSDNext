//! 事件类型定义：对应 TTHSD DLL 回调中传递的 JSON 结构

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// DLL 回调中 event 参数的 JSON 结构
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DownloadEvent {
    #[serde(rename = "Type")]
    pub event_type: String,
    #[serde(rename = "Name", default)]
    pub name: String,
    #[serde(rename = "ShowName", default)]
    pub show_name: String,
    #[serde(rename = "ID", default)]
    pub id: String,
}

/// DLL 回调中 data 参数的 JSON 反序列化结果
pub type EventData = HashMap<String, serde_json::Value>;

/// 封装好的下载事件消息（通过 mpsc channel 发送给调用方）
#[derive(Debug, Clone)]
pub struct DownloadEventMsg {
    pub event: DownloadEvent,
    pub data: EventData,
}
