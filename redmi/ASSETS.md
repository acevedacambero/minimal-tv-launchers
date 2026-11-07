# 1.6 界面素材

背景使用内置 image_gen 工具生成，已将选定原图完整复制到项目 `app/res/drawable-nodpi/wallpaper_lake.png`，APK 直接打包此 PNG，不依赖联网或外部图片路径。原图保留在生成工具输出目录。背景为蓝调山湖摄影风格，文字、卡片与时钟均由原生 UI 绘制。

BT、Clash Meta、SmartTube、Televizo 图标在运行时通过 Android PackageManager 读取已安装应用的原生图标，没有复制或重绘其商标。HDMI、应用列表、系统设置、显示器设置及恢复入口采用 `SystemIconDrawable.java` 的统一矢量绘制，随焦点调整颜色。没有额外图标库或网络请求。

生成模式：内置 image_gen，生成新图，非 CLI/API 回退。

最终生成提示词：

```
Use case: photorealistic-natural. Asset type: 16:9 landscape wallpaper for a minimal Android TV launcher, project-bound background image only. Primary request: a beautiful quiet mountain lake at blue hour with subtle mist between layered mountain ridges, dark evergreen slopes reflected in perfectly still water. Style: premium editorial landscape photography, restrained, natural detail, serene and cinematic without exaggeration. Composition: panoramic 16:9, distant mountains around the middle, broad calm water in the lower half and uncluttered sky above, no close foreground subject. Lighting: soft blue-hour light, charcoal and deep navy shadows with muted teal reflections; comfortably dark overall to support white interface text and translucent cards laid over it. Constraints: image only, no UI, no lettering, no logos, no watermark, no people, no buildings, no neon, no oversaturated highlights. Deliver a high-resolution 16:9 landscape image.
```
