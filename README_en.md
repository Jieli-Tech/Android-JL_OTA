[tag download]:https://github.com/Jieli-Tech/Android-JL_OTA/tags
[tag_badgen]:https://img.shields.io/github/v/tag/Jieli-Tech/Android-JL_OTA?style=plastic&logo=android&labelColor=ffffff&color=informational&label=Tag&logoColor=blue

# Android-JL_OTA  [![tag][tag_badgen]][tag download]

<div align="center">

**JieLi OTA SDK (Android) - An Integrated SDK for Firmware Upgrade of JieLi Bluetooth Products**

[中文](./README.md) · [English](./README_en.md) · [Documentation Center](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/index.html) · [SDK Changelog](#8-version-history) · [Report Issues](https://github.com/Jieli-Tech/Android-JL_OTA/issues)

</div>

---

## Table of Contents

- [1. Overview](#1-overview)
- [2. Environment Requirements](#2-environment-requirements)
- [3. Quick Start](#3-quick-start)
- [4. Project Structure](#4-project-structure)
- [5. Configuration Guide](#5-configuration-guide)
- [6. Debugging Tips](#6-debugging-tips)
- [7. Community & Support](#7-community--support)
- [8. Version History](#8-version-history)
- [9. License](#9-license)

---

## 1. Overview

`Android-JL_OTA` is a firmware upgrade development platform provided by **Zhuhai Jieli Technology Co., Ltd.** for JieLi Bluetooth products. This SDK is specifically designed to implement <strong style="color:red">RCSP OTA</strong> upgrade functionality for our company's Bluetooth products, supporting multiple transport methods such as BLE and SPP, and providing a complete firmware upgrade workflow.

**JieLi OTA SDK** provides a rich set of upgrade features:

| Feature | Description |
| -------------- | -------------------------------------------------------- |
| **BLE Upgrade** | Firmware upgrade via BLE channel, supporting Gatt Over BR/EDR |
| **SPP Upgrade** | Firmware upgrade via classic Bluetooth SPP channel |
| **Auto Reconnect** | Automatic BLE reconnection for single-backup OTA, improving user experience |
| **Reuse Space Upgrade** | Supports special upgrade process for reused space |

---

## 2. Environment Requirements

| Category | Requirement | Description |
|------|------------|------|
| **Operating System** | Android 5.1+ | Supports BLE functionality |
| **Hardware** | JieLi SDK with **RCSP OTA** support | AC707N, AC703N, AC701N, AC697N, AC696N, AC695N, etc. |
| **Development Platform** | Android Studio | Latest version recommended |
| **Language Support** | Java/Kotlin | Full API support provided |

---

## 3. Quick Start

### 3.1 Clone the Repository

```bash
git clone https://github.com/Jieli-Tech/Android-JL_OTA.git
cd Android-JL_OTA
```

### 3.2 Import the Project into Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the extracted `code/` directory
4. Open the project files in the reference Demo source code project

### 3.3 Add Dependencies

- **jl_bt_ota_Vxxx-release.aar**: Core OTA upgrade library, including RCSP protocol processing, upgrade workflow control, and other features

**Note: xxx represents the version number**

Add the AAR file from the `libs/` directory to your project's `libs` folder, and add the dependency in `build.gradle`:

```gradle
dependencies {
    // 1. Place the above aar file into the corresponding module's lib folder
    // 2. Add to the module's build.gradle
    implementation fileTree(include: ['*.aar'], dir: 'libs')
}
```

### 3.4 Permission Configuration

When integrating the SDK, the following permissions should be declared in `AndroidManifest.xml`:

```xml
<!-- Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

<!-- Required for newer Android versions -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Location permission: officially required for Bluetooth or network development -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Storage permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### 3.5 Run the Sample Application

Refer to the test APKs in the `apk/` directory to learn about SDK features and usage.

---

## 4. Project Structure

```
Android-JL_OTA/
├── apk/                                # Test APK folder
│   ├── JLOTA_V1.9.0_10905-debug.apk         # OTA test version
│   └── UpdateContent.txt                    # Update notes
├── code/                                    # Reference source code project folder
│   └── 参考Demo源码工程                  # OTA Demo project source code
├── doc/                                     # Documentation folder
│   ├── JieLi_OTA_SDK_Android_Development_Doc # JieLi OTA External Library (Android) Dev Docs
│   └── 杰理OTA外接库(Android)开发文档链接     # OTA online development docs link
├── libs/                               # Core library folder
│   ├── jl_bt_ota_V1.11.0_11015-release.aar  # JieLi OTA core library
│   └── ReadMe.txt                           # Core library instructions
└── ReadMe.txt                          # Instructions file
```

---

## 5. Configuration Guide

### 5.1 OTA Parameter Configuration

When integrating the SDK, OTA-related parameters need to be configured in code. For detailed instructions, refer to [Configure OTA Parameters](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/development/development_desc.html#configure-ota-option).

```java
OTAManager otaManager = new OTAManager();
BluetoothOTAConfigure bluetoothOption = BluetoothOTAConfigure.createDefault();
bluetoothOption.setPriority(BluetoothOTAConfigure.PREFER_BLE) // Choose according to your project needs
            .setUseAuthDevice(true) // Choose based on firmware configuration
            .setBleIntervalMs(500) // Default is 500ms
            .setTimeoutMs(3000) // Command timeout
            .setMtu(500) // BLE underlying communication MTU value, which affects BLE data transmission rate. Recommended values: 500 or 270. This MTU value causes the OTA library to change the MTU at BLE connection time, so the client SDK needs to handle this accordingly.
            .setNeedChangeMtu(false) // No need to adjust MTU; it is recommended that the client adjusts the BLE MTU at connection time
            .setUseReconnect(false); // Whether to use custom reconnection method. Default is false, using the SDK's default reconnection method. Clients can change this based on their needs.
bluetoothOption.setFirmwareFilePath(firmwarePath); // Set the path to the OTA firmware file stored locally
//        bluetoothOption.setFirmwareFileData(firmwareData); // Set the OTA firmware file data stored locally. Either this or setFirmwareFilePath — choose one.
otaManager.configure(bluetoothOption); // Set OTA parameters
```

#### 5.1.1 BluetoothOTAConfigure

Bluetooth OTA library configuration

| Property Name | Type | Description |
| --------------------------- | --------------- | ------------------------------------------------------------ |
| priority | int | OTA communication method<br />0 - BluetoothOTAConfigure#PREFER_BLE (default)<br />1 - BluetoothOTAConfigure#PREFER_SPP |
| isUseReconnect | boolean | Whether to use custom reconnection method<br />Default is false, disabled |
| isUseAuthDevice | boolean | Whether to enable device authentication<br />Default is true, enabled |
| `firmwareFilePath` | String | Firmware upgrade file storage path<br />`Defaults to empty, must be set before upgrade` |
| `firmwareFileData` | byte[] | Firmware upgrade file data<br />`Defaults to empty, must be set before upgrade`<br />Same as **firmwareFilePath**, either one is sufficient |
| mtu | int | Adjusted BLE MTU value<br />Range: [20, 509], default is 20 |
| isNeedChangeMtu | boolean | Whether MTU adjustment is needed<br />Default is false, no adjustment |
| bleScanMode | int | BLE scan mode<br />0 — Low Power Mode<br />1 — Balanced Mode (default)<br />2 — Low Latency Mode (high power consumption, foreground only) |
| snGenerator | ICmdSnGenerator | Command SN generator<br />If null, the default SN generator is used. Suitable for scenarios where multiple JieLi libraries are used together. |
| isPriorityCallbackOtaFinish | boolean | Whether to prioritize the OTA completion callback<br />Default is false. The OTA completion status will be callback after the device restarts. |
| bleConnectParam | BleConnectParam | BLE connection parameters<br />Parameters for automatic BLE reconnection. Default is **null**, disabling automatic BLE connection.<br />Notes: 1. If **isUseReconnect** is `true`, this field will be **ignored**.<br />2. If **isUseReconnect** is `false` and this field is not null, the OTA library will **automatically reconnect**.<br />3. If **isUseReconnect** is `false` and this field is null, the client needs to implement the `connectBluetoothDevice` interface. |

### 5.2 Usage Flow

1. **Open the APP** - On first launch, grant Bluetooth, storage, and other required permissions
2. **Add Upgrade File** - Supports the following methods:
   - Copy the upgrade file to the fixed location: `Phone root directory/Android/data/com.jieli.otasdk/files/upgrade/`
   - Place in the phone's `Download` folder, then select the local file
   - Transfer files to the phone via local network
3. **Connect Target Device** - Search and connect to the Bluetooth device to upgrade
4. **Start OTA Upgrade** - Select the target upgrade file and start the OTA upgrade

---

## 6. Debugging Tips

- **Log Output**: The SDK provides detailed log output, allowing you to view OTA connection status and data interactions via logs.
- **Device Debugging**: Use **Android Studio**'s `Logcat` to view real-time logs.
- **Troubleshooting**:
  - **SDK**: Refer to [SDK Debugging Guide](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/other/debug.html)
  - **FAQ**: Refer to [Frequently Asked Questions](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/other/qa.html)

---

## 7. Community & Support

### 7.1 Technical Exchange

| Platform | Contact | Status |
|------|----------|------|
| **Official Website** | [JieLi Technology](https://www.zh-jieli.com/) | ✅ Active |
| **GitHub Issues** | [Issue Tracker](https://github.com/Jieli-Tech/Android-JL_OTA/issues) | ✅ Active |

### 7.2 Resource Links

| Resource | Link |
|------|------|
| 📖 **Online Documentation Center** | [JieLi OTA SDK Development Docs](https://doc.zh-jieli.com/Apps/Android/ota/en-us/master/index.html) |
| 📄 **Datasheets** | [Development Docs](./doc/) |
| 📚 **Version History** | [Version History](#8-version-history) |
| 🐛 **Issue Tracking** | [GitHub Issues](https://github.com/Jieli-Tech/Android-JL_OTA/issues) |

---

## 8. Version History

| Version | Date | Change Log |
|------|------|----------|
| 1.11.0 | 2026/01/30 | 1. New Features<br />1.1 Added support for special upgrade process for reused space<br />1.2 Added automatic BLE reconnection feature for single-backup OTA<br />1.3 Added support for Gatt Over BR/EDR connection method<br />2. Optimizations<br />2.1 Added Android 15 compatibility handling |
| 1.10.0 | 2025/08/11 | 1. Fixed storage permission request failure on Android 14+ phones<br />2. Fixed local network file transfer IP address error |
| 1.10.0 | 2025/06/04 | 1. Fixed SPP mode single-backup OTA failure issue<br />2. Added Android 14 compatibility handling<br />3. Refactored APP UI framework |
| 1.9.3 | 2024/01/26 | 1. Added x86 and x86_64 platform support<br />2. Fixed slow BLE data transmission issue |
| 1.9.2 | 2023/03/29 | 1. Fixed packet assembly error causing data loss issue<br />2. Added Android 13 compatibility handling |
| 1.9.0 | 2022/12/17 | 1. Fixed device reconnection failure issue<br />2. Fixed SPP mode OTA failure<br />3. Fixed dual-mode same-address device OTA failure<br />4. Fixed TWS earphone single-backup OTA failure<br />5. Supported multi-device upgrades (removed singleton usage, processes independent)<br />6. Added Android 11 compatibility handling |
| 1.6.0 | 2022/04/07 | 1. Added new reconnection method<br />2. Added protocol MTU adjustment at device startup<br />3. Fixed multithreaded command sending with same SN issue<br />4. Fixed RCSP authentication process data anomaly issue |

---

## 9. License

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
