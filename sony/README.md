# Sony 极简桌面 1.2.2

实测 Sony BRAVIA VH1，Android 10 / API 29，固件 `QTG3.200305.006.S387`，包名 `local.minimal.sony`。

| 位置 | 从左到右 |
| --- | --- |
| 首页第一排 | 电视、银河奇异果、VidHub、Televizo |
| 首页第二排 | B站、Clash Meta、SmartTube、OwnTV |
| 第二页 | 信号源、电视设置、应用列表、Disney+ |

首页第二排按下键翻页。第二页按上键返回首页。五张离线背景按电视时钟每小时整点切换。 [背景素材](ASSETS.md) · [武汉天气](WEATHER.md)

「电视」直达 HDMI 1。「信号源」可以选择原生电视播放器的上次输入或 HDMI 1–4。开机进入默认 HOME 时，桌面检测 HDMI 1 的端口与视频状态，信号可用时打开原生播放器。手动按主页键可打开桌面。

## 安装和恢复

在 `sony/` 下创建 `dist/`，放入 Releases 的 `minimal-sony.apk` 和 `minimal-sony.apk.sha256`。在 PowerShell 7 中把地址替换成电视的实际 ADB 地址，脚本也接受 `-Device` 和 `-Adb`。

```powershell
$env:TV_ADB_SERIAL = '电视IP:5555'
adb connect $env:TV_ADB_SERIAL
.\scripts\Install.ps1
```

先检查 HDMI 1、应用列表与遥控器导航，再切换默认桌面。

```powershell
.\scripts\Set-Home.ps1
```

应用列表里的「原桌面」可打开实测设备的旧桌面。运行 `.\scripts\Restore.ps1` 会根据首次备份恢复原来的默认 HOME。
