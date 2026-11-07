# Sony 极简桌面界面素材

1.2 起的五张背景使用内置 image_gen 工具分别生成。成品为 1672×941 WebP，放在 `app/res/drawable-nodpi`，随 APK 离线使用。文字、卡片与时钟由原生 UI 绘制。

BT、Clash Meta、SmartTube、Televizo 图标在运行时通过 Android PackageManager 读取已安装应用的原生图标，没有复制或重绘其商标。HDMI、应用列表、系统设置、显示器设置及恢复入口采用 `SystemIconDrawable.java` 的统一矢量绘制，随焦点调整颜色。没有额外图标库或网络请求。

## 1.2 生成提示词

### alpine

```text
Use case: photorealistic-natural. Generate a standalone cinematic landscape photograph for a Sony TV launcher background, full-bleed ultra-wide landscape 16:9 framing. Scene: Quiet alpine lake at deep blue hour, a distant line of snow-streaked peaks and dark pine forest reflected in calm water, subtle indigo and teal. Composition: keep the top corners quiet and moderately dark for white weather and clock text, with a broad uncluttered composition that supports two rows of translucent tiles across the middle. Natural soft contrast, rich restrained colors, detailed but low visual noise, no harsh hotspots, no people, no buildings, no text, no logos, no watermark, no interface, no frame. One single standalone image.
```

### bamboo

```text
Use case: photorealistic-natural. Generate a standalone cinematic landscape photograph for a Sony TV launcher background, full-bleed ultra-wide landscape 16:9 framing. Scene: A wide, misty bamboo woodland seen through airy layers, soft jade and charcoal greens, a narrow forest path dissolving into fog. Soft natural texture rather than a dense pattern. Composition: keep the top corners quiet and moderately dark for white weather and clock text, with a broad uncluttered composition that supports two rows of translucent tiles across the middle. Natural soft contrast, rich restrained colors, detailed but low visual noise, no harsh hotspots, no people, no buildings, no text, no logos, no watermark, no interface, no frame. One single standalone image.
```

### coast

```text
Use case: photorealistic-natural. Generate a standalone cinematic landscape photograph for a Sony TV launcher background, full-bleed ultra-wide landscape 16:9 framing. Scene: A high viewpoint over a wild rocky ocean coast at dusk, graceful curving cliffs and long-exposure slate-blue waves, violet and deep petrol blue. Composition: keep the top corners quiet and moderately dark for white weather and clock text, with a broad uncluttered composition that supports two rows of translucent tiles across the middle. Natural soft contrast, rich restrained colors, detailed but low visual noise, no harsh hotspots, no people, no buildings, no text, no logos, no watermark, no interface, no frame. One single standalone image.
```

### dunes

```text
Use case: photorealistic-natural. Generate a standalone cinematic landscape photograph for a Sony TV launcher background, full-bleed ultra-wide landscape 16:9 framing. Scene: Vast wind-shaped desert dunes just after sunset, sweeping sculptural forms, subdued copper, bronze, and plum shadows, no direct sun. Composition: keep the top corners quiet and moderately dark for white weather and clock text, with a broad uncluttered composition that supports two rows of translucent tiles across the middle. Natural soft contrast, rich restrained colors, detailed but low visual noise, no harsh hotspots, no people, no buildings, no text, no logos, no watermark, no interface, no frame. One single standalone image.
```

### aurora

```text
Use case: photorealistic-natural. Generate a standalone cinematic landscape photograph for a Sony TV launcher background, full-bleed ultra-wide landscape 16:9 framing. Scene: A remote snowy plateau at night with distant mountains and a restrained soft green aurora stretching gently over a deep blue sky, small natural stars. Composition: keep the top corners quiet and moderately dark for white weather and clock text, with a broad uncluttered composition that supports two rows of translucent tiles across the middle. Natural soft contrast, rich restrained colors, detailed but low visual noise, no harsh hotspots, no people, no buildings, no text, no logos, no watermark, no interface, no frame. One single standalone image.
```

## 旧版山湖背景提示词

```
Use case: photorealistic-natural. Asset type: 16:9 landscape wallpaper for a minimal Android TV launcher, project-bound background image only. Primary request: a beautiful quiet mountain lake at blue hour with subtle mist between layered mountain ridges, dark evergreen slopes reflected in perfectly still water. Style: premium editorial landscape photography, restrained, natural detail, serene and cinematic without exaggeration. Composition: panoramic 16:9, distant mountains around the middle, broad calm water in the lower half and uncluttered sky above, no close foreground subject. Lighting: soft blue-hour light, charcoal and deep navy shadows with muted teal reflections; comfortably dark overall to support white interface text and translucent cards laid over it. Constraints: image only, no UI, no lettering, no logos, no watermark, no people, no buildings, no neon, no oversaturated highlights. Deliver a high-resolution 16:9 landscape image.
```
