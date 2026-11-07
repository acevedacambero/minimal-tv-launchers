# 极简电视桌面 · Minimal TV Launchers

两个独立的 Android TV 桌面。支持遥控器方向键、确认和返回，提供常用应用快捷入口、HDMI 和武汉天气。设备配置通过 ADB 完成，无需 root。

| 版本 | 实测设备 | 主页键与输入源 |
| --- | --- | --- |
| [红米 2.2](redmi/README.md) | MiTV-MFFU1，Android 14，`OS3.0.109.0.UFFMAMN` | 可选主页键服务、HDMI 1、两排八个入口 |
| [Sony 1.2.2](sony/README.md) | BRAVIA VH1，Android 10，`QTG3.200305.006.S387` | 系统默认 HOME、开机有信号时切换 HDMI 1、两页十二个入口、整点轮换背景 |

## 下载与安装

从 [Releases](https://github.com/acevedacambero/minimal-tv-launchers/releases/latest) 下载对应的 APK 和同名 `.sha256` 文件。Windows 安装脚本需要 PowerShell 7 和 Android SDK Platform Tools。把 `adb.exe` 所在目录加入 `PATH`，或向脚本传入 `-Adb`。脚本会核对实机型号、构建版本和 APK 校验值。

- [红米安装、主页键与恢复](redmi/README.md)
- [Sony 安装、默认桌面与恢复](sony/README.md)

## 从源码构建

在 Windows 上安装 JDK 17 或 21、Android SDK Platform 34、Build Tools 36.0.0。用 PowerShell 7 在仓库根目录运行以下命令。设置 `JAVA_HOME` 和 `ANDROID_HOME`，或通过 `-Jdk`、`-Sdk` 传入路径。

```powershell
pwsh -File .\Build.ps1 -Variant redmi
pwsh -File .\Build.ps1 -Variant sony
```

APK 和 SHA-256 文件分别写入 `redmi/dist/` 与 `sony/dist/`。本地构建使用本机生成的开发签名，覆盖已安装版本时需保持签名一致。设备首次备份和操作日志写入对应版本的 `state/` 目录。

## 许可

项目采用 [MIT 许可证](LICENSE)。应用图标由系统读取已安装应用的资源，天气数据采用 Open-Meteo 的 CC BY 4.0 许可，免费 API 适用于非商业用途。 [Open-Meteo 数据许可](https://open-meteo.com/en/licence) · [API 条款](https://open-meteo.com/en/terms)
