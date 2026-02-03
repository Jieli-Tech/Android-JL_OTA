# Android-JL_OTA

The bluetooth OTA for Android

[中文](https://github.com/Jieli-Tech/Android-JL_OTA/blob/master/README.md) | **English**



<br/>

<div align="center">

![Android](https://img.shields.io/badge/Android-5.1+-blue.svg)
![Android Studio](https://img.shields.io/badge/Android_Studio-Latest-orange.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)       

<strong style="font:24px;color:#000000">JieLi OTA SDK (Android)</strong>

**JieLi OTA SDK** is an integrated SDK developed by <strong style="color:#ee2233">Zhuhai JieLi  Technology Co., Ltd.</strong> (hereinafter referred to as "our company") specifically for implementing upgrade functions for our company's Bluetooth products.

</div>



## Quick Start

To help developers quickly integrate the <strong style="color:red">JieLi  OTA SDK</strong>, please read carefully before development:

-  [Jieli OTA External Library Development Documentation (Android)](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/index.html)
- [Configure OTA Parameters](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/development/development_desc.html#configuring-ota-parameters)
- [Debugging Instructions](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/other/debug.html#debug-instructions)



## Integration Q&A

Common questions and answers based on developer feedback are provided for unified responses. When encountering problems, developers can first refer to [Frequently Asked Questions](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/other/qa.html#faq).<br/> If the problem still cannot be solved, please submit an issue and we will respond as soon as possible.



## Changelog

| Date       | Version                | Release Content                                              | Responsible Person             |
| ---------- | ---------------------- | ------------------------------------------------------------ | ------------------------------ |
| 2026/01/30 | APP_V1.9.0_SDK_V1.11.0 | 1. New Features<br />1.1 Added support for special upgrade process for reusing space<br />1.2 Added automatic BLE reconnection feature for single backup OTA<br />1.3 Added support for Gatt Over BR/EDR connection method<br />2. Optimized Features<br />2.1 Added compatibility handling for Android 15 | ZhuoCheng Zhong                |
| 2025/08/11 | APP_V1.8.1_SDK_V1.10.0 | 1. Fixed storage permission request failure on Android 14+ phones<br />2. Fixed local network file transfer IP address error | ZhuoCheng Zhong                |
| 2025/06/04 | APP_V1.8.0_SDK_V1.10.0 | 1. Fixed SPP mode single backup OTA failure issue<br />2. Added Android 14 compatibility handling<br />3. Refactored APP UI framework | ZhuoCheng Zhong                |
| 2024/01/26 | APP_V1.7.1_SDK_V1.9.3  | 1. Added x86 and x86_64 platform support<br />2. Fixed slow BLE data transmission issue | ZhuoCheng Zhong                |
| 2023/03/29 | APP_V1.7.0_SDK_V1.9.2  | 1. Fixed packet assembly error causing data loss issue<br />2. Added Android 13 compatibility handling | ZhuoCheng Zhong                |
| 2022/12/17 | APP_V1.6.0_SDK_V1.9.0  | 1. Fixed device reconnection failure issue<br />2. Fixed SPP mode OTA failure<br />3. Fixed dual-mode same address device OTA failure<br />4. Fixed TWS earphone single backup OTA failure<br />5. Supported multi-device upgrades (removed singleton usage, processes independent)<br />6. Added Android 11 compatibility handling | ZhuoCheng Zhong                |
| 2022/04/07 | APP_V1.5.0_SDK_V1.6.0  | 1. Added new reconnection method<br />2. Added MTU adjustment for device startup protocol<br />3. Fixed multithreaded command sending with same SN issue<br />4. Fixed RCSP authentication process data anomaly issue | HuanMing Zhang/ZhuoCheng Zhong |



## Package File Structure

```tex
apk  --- Test APK
 ├── JLOTA_V1.9.0_10905-debug.apk
 ├── UpdateContent.txt
code --- Demo Program Source Code
 ├── Reference Demo source code projec
doc  --- Development Documentation
 ├── 杰理OTA外接库(Android)开发文档链接
 ├── JieLi_OTA_SDK_Android_Development_Doc
libs --- Core Libraries
 ├── jl_bt_ota_V1.11.0_11015-release.aar
 └── ReadMe.txt
```



## Usage Instructions

1. Open the APP (for first-time use, corresponding permissions need to be granted)<br/>

2. Add upgrade file<br/>

   a.  Copy the upgrade file to the fixed storage location on your phone: `Phone root directory/Android/data/com.jieli.otasdk/files/upgrade/`<br/>

   b. Store in the phone's ``Download`` folder, then select local file<br/>

   c. Transfer files to phone via local network <br/>

3. Connect to target upgrade device<br/>

4. Select target upgrade file and start OTA upgrade



## Technical Support

- 🌐 **Official Website**: [JieLi Technology](https://www.zh-jieli.com/)
- 📧 **Technical Support**: Please contact through official channels





## License

This project is licensed under the [Apache License 2.0](./LICENSE).

```
Copyright 2024 Zhuhai Jieli Technology Co., Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

<div align="center">

**© 2024 Zhuhai Jieli Technology Co., Ltd. | Licensed under Apache License 2.0**

</div>
