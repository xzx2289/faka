# 移动端 Bootstrap API 协议（拟新增接口，不是 ACG-Faka 现成接口）

## Endpoint
`GET /mobile-api/v1/bootstrap`

必须通过 HTTPS。此 endpoint 是本项目约定的新接口；部署前需要在你的 ACG-Faka 版本中新增服务端实现和权限/限流配置。

## 示例响应
```json
{
  "ok": true,
  "app": {
    "latest_version_name": "0.2.0",
    "latest_version_code": 2,
    "min_supported_version_code": 1,
    "force_update": false,
    "update_url": "https://example.com/download/acg-faka.apk"
  },
  "announcement": {
    "enabled": true,
    "id": "notice-2026-10",
    "title": "维护公告",
    "body": "今晚 23:00 进行短暂维护。",
    "force": false
  }
}
```

## 字段语义
- `latest_version_code`: 服务器当前推荐版本的整数版本号。
- `min_supported_version_code`: 低于此版本必须阻止进入业务界面。
- `force_update`: 全局强制更新开关；开启后当前旧客户端必须更新。
- `update_url`: HTTPS 下载地址或受信任的应用分发页。
- `announcement.enabled`: 是否显示公告。
- `announcement.id`: 公告唯一 ID，用于客户端记录已读状态（当前示例尚未持久化已读状态）。
- `announcement.force`: true 时禁止点背景/返回键关闭，只能点击确认按钮；这不代表用户确实阅读了公告。
- `announcement.title/body`: 纯文本，不要作为 HTML 渲染。

## 服务端必须做
1. 对 `latest_version_code`、`min_supported_version_code` 做整数校验。
2. 下载链接使用 HTTPS 且限制为可信域名。
3. 这个接口只返回版本策略和公告，不包含管理员密钥、数据库密码或插件内部配置。
4. 强制更新不是 Android 系统级强制安装。客户端只能阻止继续使用并引导到更新页；实际安装仍需用户确认，除非使用受管理设备/企业分发机制。
5. 公告已读状态最好由客户端本地按公告 ID 保存；若要求跨设备统计，再增加已授权的 ACK 接口。
6. 不要仅依赖客户端校验保护业务数据；所有管理员操作都必须由服务器再次验证权限。
7. 对业务模块另行建立真实 API 映射，不得把公开下单 API 当成管理员 API。
