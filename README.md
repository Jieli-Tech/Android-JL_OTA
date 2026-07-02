[tag download]:https://github.com/Jieli-Tech/Android-JL_OTA/tags
[tag_badgen]:https://img.shields.io/github/v/tag/Jieli-Tech/Android-JL_OTA?style=plastic&logo=android&labelColor=ffffff&color=informational&label=Tag&logoColor=blue

# Android-JL_OTA  [![tag][tag_badgen]][tag download]

<div align="center">

**杰理OTA SDK(Android) - 专为杰理蓝牙类产品提供固件升级功能的集成SDK**

[中文](./README.md) · [English](./README_en.md) · [文档中心](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/index.html) · [SDK 版本历史](#八版本历史) · [报告问题](https://github.com/Jieli-Tech/Android-JL_OTA/issues)

</div>

---

## 📋 目录

- [一、概述](#一概述)
- [二、运行环境](#二运行环境)
- [三、快速开始](#三快速开始)
- [四、工程结构](#四工程结构)
- [五、配置说明](#五配置说明)
- [六、调试技巧](#六调试技巧)
- [七、社区与支持](#七社区与支持)
- [八、版本历史](#八版本历史)
- [九、许可证](#九许可证)

---



## 一、概述

`Android-JL_OTA` 是**珠海市杰理科技股份有限公司**为杰理蓝牙类产品提供的固件升级开发平台。本 SDK 专门实现本公司蓝牙类产品的 <strong style="color:red">RCSP OTA</strong> 升级功能，支持 BLE、SPP 等多种传输方式，提供完整的固件升级流程。

**杰理OTA SDK**提供了丰富的升级功能：

| 功能           | 说明                                                     |
| -------------- | -------------------------------------------------------- |
| **BLE升级** | 通过BLE通道进行固件升级，支持``Gatt Over BR/EDR``方式 |
| **SPP升级**   | 通过经典蓝牙SPP通道进行固件升级                  |
| **自动回连**   | 单备份OTA自动回连BLE功能，提升用户体验                           |
| **复用空间升级** | 支持复用空间特殊升级流程                             |

---



## 二、运行环境

| 类别 | 要求 | 说明 |
|------|------------|-----------|
| **操作系统** | Android 5.1+ | 支持BLE功能 |
| **硬件要求** | 支持**RCSP OTA**功能的杰理SDK | AC707N、AC703N、AC701N、AC697N、AC696N、AC695N等 |
| **开发平台** | Android Studio | 建议使用最新版本 |
| **语言支持** | Java/Kotlin | 提供完整的API支持 |


---



## 三、快速开始

### 3.1 克隆仓库

```bash
git clone https://github.com/Jieli-Tech/Android-JL_OTA.git
cd Android-JL_OTA
```



### 3.2 导入项目到Android Studio

1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 导航到解压后的 `code/` 目录
4. 打开参考Demo源码工程中的项目文件



### 3.3 添加依赖库

- **jl_bt_ota_Vxxx-release.aar** : OTA升级核心库，包含RCSP协议处理、升级流程控制等功能

**PS: xxx为版本号**

将 `libs/` 目录下的 AAR 文件添加到项目的 `libs` 目录中，并在 `build.gradle` 中添加依赖：

```gradle
dependencies {
    //1.将上面的aar文件放入工程目录中的对应moudle的lib文件夹下
	//2.在moudlu的build.gradle中添加
	implementation fileTree(include: ['*.aar'], dir: 'libs')
}
```



### 3.4 权限配置

接入SDK时应在 `AndroidManifest.xml` 申请以下权限:

```xml
<!--使用蓝牙权限-->
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

<!--高版本安卓系统要求-->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!--定位权限，官方要求使用蓝牙或网络开发，需要位置信息-->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!--存储权限-->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```



### 3.5 运行示例应用

参考 `apk/` 目录中的测试APK，了解SDK功能和使用方法。



---



## 四、工程结构

```
Android-JL_OTA/
├── apk/                                # 测试APK文件夹
│   ├── JLOTA_V1.9.0_10905-debug.apk         # OTA测试版本
│   └── UpdateContent.txt                    # 更新说明
├── code/                                    # 参考源码工程文件夹
│   └── 参考Demo源码工程                  # OTA Demo项目源码
├── doc/                                     # 开发文档文件夹
│   ├── JieLi_OTA_SDK_Android_Development_Doc # 杰理OTA外接库(Android)开发文档
│   └── 杰理OTA外接库(Android)开发文档链接        # OTA在线开发文档地址
├── libs/                               # 核心库文件夹
│   ├── jl_bt_ota_V1.11.0_11015-release.aar  # 杰理OTA核心库
│   └── ReadMe.txt                           # 核心库说明文件
└── ReadMe.txt                          # 说明文件
```

---



## 五、配置说明

### 5.1 OTA参数配置

接入SDK时，需在代码中配置OTA相关参数，详细说明请参考 [配置OTA参数](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/development/development_desc.html#configure-ota-option)。

```java
OTAManager otaManager = new OTAManager();
BluetoothOTAConfigure bluetoothOption = BluetoothOTAConfigure.createDefault();
bluetoothOption.setPriority(BluetoothOTAConfigure.PREFER_BLE) //请按照项目需要选择
            .setUseAuthDevice(true) //具体根据固件的配置选择
            .setBleIntervalMs(500) //默认是500毫秒
            .setTimeoutMs(3000) //命令超时时间
            .setMtu(500) //BLE底层通讯MTU值，会影响BLE传输数据的速率。建议用500 或者 270。该MTU值会使OTA库在BLE连接时改变MTU，所以用户SDK需要对此处理。
            .setNeedChangeMtu(false) //不需要调整MTU，建议客户连接时调整好BLE的MTU
            .setUseReconnect(false); //是否自定义回连方式，默认为false，走SDK默认回连方式，客户可以根据需求进行变更
bluetoothOption.setFirmwareFilePath(firmwarePath); //设置本地存储OTA文件的路径
//        bluetoothOption.setFirmwareFileData(firmwareData);//设置本地存储OTA文件的数据, 与setFirmwareFilePath，二者选其一
otaManager.configure(bluetoothOption); //设置OTA参数
```



#### 5.1.1 BluetoothOTAConfigure

蓝牙OTA库配置

| 属性名                      | 类型            | 描述                                                         |
| --------------------------- | --------------- | ------------------------------------------------------------ |
| priority                    | int             | OTA的通讯方式<br />0 - BluetoothOTAConfigure#PREFER_BLE(默认值)<br />1 - BluetoothOTAConfigure#PREFER_SPP |
| isUseReconnect              | boolean         | 是否使用自定义回连方式<br />默认值是false， 不使用           |
| isUseAuthDevice             | boolean         | 是否启用设备认证<br />默认值是true, 开启设备认证             |
| `firmwareFilePath`          | String          | 固件升级文件存放路径<br />`默认为空，升级前需要设置`         |
| `firmwareFileData`          | byte[]          | 固件升级文件数据<br />`默认为空，升级前需要设置`<br />与 **firmwareFilePath** 一样, 两者选其一即可 |
| mtu                         | int             | 调节后的BLE的MTU值<br />取值范围：[20, 509]， 默认值是20     |
| isNeedChangeMtu             | boolean         | 是否需要调节MTU<br />默认值是false, 不调节mtu                |
| bleScanMode                 | int             | BLE扫描模式<br />0 — 低功耗模式<br />1 — 平衡模式(默认值)<br />2 — 低延时模式(高功耗，仅前台有效) |
| snGenerator                 | ICmdSnGenerator | 命令SN生成器<br />若为null,则采用默认SN生成器, 适用于杰理多库联合使用 |
| isPriorityCallbackOtaFinish | boolean         | 是否优先回调OTA结束状态<br />默认值是false, OTA结束状态将在设备重启后回调。 |
| bleConnectParam             | BleConnectParam | BLE连接参数<br />设置自动回连BLE的参数，默认值是 **null**， 关闭自动连接BLE功能<br />说明：1. 如果 **isUseReconnect** 为 `true`， 该字段 `不生效`<br />2. 如果 **isUseReconnect** 为 `false`, 且该字段不为空，则OTA库 **自动回连**<br />3. 如果 **isUseReconnect** 为 `false`, 但该字段为空，<br />则客户需要实现 `connectBluetoothDevice` 接口 |



### 5.2 使用流程

1. **打开APP** - 初次打开应用，需要授予蓝牙、存储等对应权限
2. **添加升级文件** - 支持以下方式：
   - 拷贝升级文件到固定存放位置 `手机根目录/Android/data/com.jieli.otasdk/files/upgrade/`
   - 存放到手机 `Download` 文件夹，然后选择本地文件
   - 通过局域网传输文件到手机
3. **连接目标设备** - 搜索并连接需要升级的蓝牙设备
4. **开始OTA升级** - 选择目标的升级文件，开始OTA升级

---



## 六、调试技巧

- **日志输出**：SDK提供详细的日志输出，可通过日志查看OTA连接状态和数据交互
- **设备调试**：使用**Android Studio**的``Logcat``查看实时日志
- **问题排查**：
  - **SDK：** 参考 [SDK调试说明](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/other/debug.html)
  - **常见问题：** 参考 [常见问题答疑](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/other/qa.html)



---



## 七、社区与支持

### 7.1 技术交流

| 平台 | 联系方式 | 状态 |
|------|----------|------|
| **官方网站** | [杰理科技](https://www.zh-jieli.com/) | ✅ 活跃 |
| **GitHub Issues** | [问题反馈](https://github.com/Jieli-Tech/Android-JL_OTA/issues) | ✅ 活跃 |



### 7.2 资源链接

| 资源 | 链接 |
|------|------|
| 📖 **在线文档中心** | [杰理OTA SDK开发文档](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/index.html) |
| 📄 **数据手册** | [开发说明文档](./doc/) |
| 📚 **版本历史** | [版本历史](#八版本历史) |
| 🐛 **问题反馈** | [GitHub Issues](https://github.com/Jieli-Tech/Android-JL_OTA/issues) |

---



## 八、版本历史

| 版本 | 日期 | 修改记录 |
|------|------|----------|
| 1.11.0 | 2026/01/30 | 1. 新增功能<br />1.1 增加复用空间特殊升级流程支持<br />1.2 增加单备份OTA自动回连BLE功能<br />1.3 增加Gatt Over BR/EDR连接方式支持<br />2. 优化功能<br />2.1 增加Android 15的兼容处理 |
| 1.10.0 | 2025/08/11 | 1. 修复 Android 14+手机存储权限申请失败问题<br />2. 修复局域网文件传输 IP 地址错误的问题 |
| 1.10.0 | 2025/06/04 | 1. 修复 SPP 方式单备份 OTA 失败问题<br />2. 增加Android 14的兼容处理<br />3. 重构APP的UI框架 |
| 1.9.3 | 2024/01/26 | 1. 增加x86 和 x86_64的平台支持<br />2. 修复BLE发数变慢的问题 |
| 1.9.2 | 2023/03/29 | 1. 修复拼包出错导致丢失数据问题<br />2. 增加Android 13的兼容处理 |
| 1.9.0 | 2022/12/17 | 1. 修复设备回连失败的问题<br />2. 修复 SPP 方式 OTA 失败<br />3. 修复双模同地址设备 OTA 失败<br />4. 修复 TWS 耳机单备份 OTA 失败<br />5. 支持多设备升级(去掉单例使用, 流程独立)<br />6. 增加Android 11的兼容处理 |
| 1.6.0 | 2022/04/07 | 1. 增加新回连方式<br />2. 增加设备启动的协议 MTU 调整<br />3. 修复多线程发命令，SN 相同的问题<br />4. 修复 RCSP 认证流程数据异常问题 |

---



## 九、许可证

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

