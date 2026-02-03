# Android-JL_OTA
The bluetooth OTA for Android

**中文** | [English](https://github.com/Jieli-Tech/Android-JL_OTA/blob/master/README_en.md)



<br/>

<div align="center">
 
![Android](https://img.shields.io/badge/Android-5.1+-blue.svg)
![Android Studio](https://img.shields.io/badge/Android_Studio-Latest-orange.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)       

<strong style="font:24px;color:#000000">杰理OTA SDK(Android)</strong>

**杰理OTA SDK**是<strong style="color:#ee2233">珠海市杰理科技股份有限公司</strong>(以下简称“本公司”)开发，专门实现本公司蓝牙类产品升级功能的集成SDK。

</div>



## 快速开始

为了帮助开发者快速接入<strong style="color:red">杰理OTA SDK</strong>，请开发前详细阅读:

-  [杰理OTA外接库开发文档(Android)](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/index.html)
- [配置OTA参数](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/development/development_desc.html#configure-ota-option)
- [调试说明](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/other/debug.html#debug)



## 接入答疑

针对开发者反馈的常见问题进行统一答疑，开发者遇到问题时，可以先参考 [常见问题答疑](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/other/qa.html)。<br/>
如果还是无法解决问题，请提交issue，我们将尽快回复。



## 更新日志

| 日期       | 版本号                 | 发布内容                                                     | 负责人        |
| ---------- | ---------------------- | ------------------------------------------------------------ | ------------- |
| 2026/01/30 | APP_V1.9.0_SDK_V1.11.0 | 1. 新增功能<br />1.1 增加复用空间特殊升级流程支持<br />1.2 增加单备份OTA自动回连BLE功能<br />1.3 增加Gatt Over BR/EDR连接方式支持<br />2. 优化功能<br />2.1 增加Android 15的兼容处理 | 钟卓成        |
| 2025/08/11 | APP_V1.8.1_SDK_V1.10.0 | 1. 修复 Android 14+手机存储权限申请失败问题<br />2. 修复局域网文件传输 IP 地址错误的问题 | 钟卓成        |
| 2025/06/04 | APP_V1.8.0_SDK_V1.10.0 | 1. 修复 SPP 方式单备份 OTA 失败问题<br />2. 增加Android 14的兼容处理<br />3. 重构APP的UI框架 | 钟卓成        |
| 2024/01/26 | APP_V1.7.1_SDK_V1.9.3  | 1. 增加x86 和 x86_64的平台支持<br />2. 修复BLE发数变慢的问题 | 钟卓成        |
| 2023/03/29 | APP_V1.7.0_SDK_V1.9.2  | 1. 修复拼包出错导致丢失数据问题<br />2. 增加Android 13的兼容处理 | 钟卓成        |
| 2022/12/17 | APP_V1.6.0_SDK_V1.9.0  | 1. 修复设备回连失败的问题<br />2. 修复 SPP 方式 OTA 失败<br />3. 修复双模同地址设备 OTA 失败<br />4. 修复 TWS 耳机单备份 OTA 失败<br />5. 支持多设备升级(去掉单例使用, 流程独立)<br />6. 增加Android 11的兼容处理 | 钟卓成        |
| 2022/04/07 | APP_V1.5.0_SDK_V1.6.0  | 1. 增加新回连方式<br />2. 增加设备启动的协议 MTU 调整<br />3. 修复多线程发命令，SN 相同的问题<br />4. 修复 RCSP 认证流程数据异常问题 | 张焕明/钟卓成 |



## 压缩包文件结构说明

```tex
apk  --- 测试APK
 ├── JLOTA_V1.9.0_10905-debug.apk
 ├── UpdateContent.txt
code --- 演示程序源码
 ├── 参考Demo源码工程
doc  --- 开发文档
 ├── 杰理OTA外接库(Android)开发文档链接
 ├── JieLi_OTA_SDK_Android_Development_Doc
libs --- 核心库
 ├── jl_bt_ota_V1.11.0_11015-release.aar
 └── ReadMe.txt
```



## 使用说明

1. 打开APP(初次打开应用，需要授予对应权限)<br>

2. 添加升级文件<br>

   a. 拷贝升级文件到手机固定的存放位置 `手机根目录/Android/data/com.jieli.otasdk/files/upgrade/`<br>

   b. 存放到手机``Download`` 文件夹，然后选择本地文件<br>

   c. 通过局域网传输文件到手机 <br>

3. 连接升级目标设备<br>

4. 选择目标的升级文件，开始OTA升级




## 技术支持

- 🌐 **官方网站**: [杰理科技](https://www.zh-jieli.com/)
- 📧 **技术支持**: 请通过官方渠道联系





## 许可证

本项目采用 [Apache License 2.0](./LICENSE) 开源协议。

```
Copyright 2024 珠海市杰理科技股份有限公司

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

**© 2024 珠海市杰理科技股份有限公司 | Licensed under Apache License 2.0**

</div>
