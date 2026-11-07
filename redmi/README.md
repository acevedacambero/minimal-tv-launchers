# 红米极简桌面 2.2

实测 MiTV-MFFU1，Android 14 / API 34，固件 `OS3.0.109.0.UFFMAMN`，包名 `local.minimal.tv`。

| 上排 | HDMI 1 | 显示器设置 | OwnTV 字幕开发版 | 应用列表 |
| --- | --- | --- | --- | --- |
| 下排 | BT | Clash Meta | SmartTube | Televizo |

HDMI 1 入口使用本机原生信号源播放器。应用快捷方式需要对应应用已安装。 [武汉天气](WEATHER.md) · [背景素材](ASSETS.md)

## 安装和恢复

在 `redmi/` 下创建 `dist/`，放入 Releases 的 `minimal-tv.apk` 和 `minimal-tv.apk.sha256`。启用电视的网络 ADB 调试，在本机用 PowerShell 7 执行。把示例地址换成电视实际的 ADB 地址，脚本也接受 `-Device` 和 `-Adb`。

```powershell
$env:TV_ADB_SERIAL = '电视IP:5555'
adb connect $env:TV_ADB_SERIAL
.\scripts\Install.ps1
```

先用遥控器确认焦点、应用列表、HDMI 1 和恢复入口。该固件的系统默认 HOME 指向原厂桌面。可选主页键服务会处理实体主页键，并通过 ADB 授予的 `WRITE_SECURE_SETTINGS` 在开机时恢复该服务配置。服务订阅原厂桌面窗口变化和按键，未启用界面内容读取。启用和恢复命令如下。

```powershell
.\scripts\Enable-HomeKey.ps1
.\scripts\Restore.ps1
```

「应用列表」里的「原厂桌面（恢复入口）」可停止主页键接管并打开原厂桌面。`Restore.ps1` 使用首次备份恢复脚本触及的包和主页键设置。

## 审计与可选停用

`Audit.ps1` 读取系统包状态。`Disable-Ads.ps1` 需要同设备、同 APK 的成功导航记录，并按 [明确的包清单](scripts/disable-whitelist.json) 调用 `pm disable-user --user 0`。首次备份中的历史停用项会跳过。安装和启用主页键时无需运行停用脚本。
