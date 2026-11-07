# 武汉天气

主页左上角显示武汉当前温度、天气状况，以及今明两天的天气和最低/最高温。城市固定为武汉市区（30.5928, 114.3055），无需定位权限，也不上传设备标识或账号。应用列表页保留“应用列表”标题。

`current` 为服务提供的当前模型天气，通常以 15 分钟为时间分辨率。`daily` 为日预报。2.2 的天气栏显示数据时刻和缓存提示，选中数据时刻并按确认键可查看来源和许可。数据来自 Open-Meteo，采用 CC BY 4.0 许可。 [预报 API](https://open-meteo.com/en/docs) · [许可](https://open-meteo.com/en/licence) · [免费接口条款](https://open-meteo.com/en/terms)

设备上的实际请求地址：

```text
https://api.open-meteo.com/v1/forecast?latitude=30.5928&longitude=114.3055&current=temperature_2m,weather_code,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%2FShanghai&forecast_days=2
```

- 通过 HTTPS 在线获取，不需要 API 密钥；新增的唯一权限是普通 INTERNET 权限。
- 桌面可见时最多每 15 分钟更新一次，失败后至少间隔 5 分钟重试；离开桌面暂停定时检查。
- 网络请求和 JSON 处理在后台线程完成，连接/读取均设置 8 秒超时；八个入口不会等待天气接口。
- 成功数据保存在私有缓存中；失败或数据较旧时显示“缓存”和原数据时刻，不用示例天气冒充实时数据。没有缓存且请求失败时明确显示“武汉天气暂不可用”。
- 当前数据时间明显异常或缺少必要字段时拒绝保存。界面温度四舍五入显示整数摄氏度，预报保留日范围。
- 不更改设备网络、VPN、图像、音频或原厂天气包，也没有天气后台服务或定时唤醒任务。

1.7 同时使用 ShortcutButton 将图标与文字作为一个整体垂直居中：图标 52 dp、文字 22 sp、间距 8 dp。继续使用原生 Button 的文本、无障碍语义、焦点和确认键行为，图标及文字不再分别贴近卡片顶部/底部。
