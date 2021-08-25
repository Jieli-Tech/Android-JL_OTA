# Android-JL_OTA
The bluetooth OTA for Android

概述
------------
 ## 压缩包文件结构说明
  apk -- 测试APK<br>
  code -- 演示程序源码<br>
  doc -- 开发文档<br>
  libs -- 核心库<br>


## 使用说明
1. 打开APP(初次打开应用，需要授予对应权限)
2. 拷贝升级文件到手机固定的存放位置<br>
  * 如果手机系统是Android 10.0+，放到/Android/data/com.jieli.otasdk/files/com.jieli.otasdk/upgrade/<br>
  * 如果手机系统是Android 10.0以下，放到/com.jieli.otasdk/upgrade/
3. 连接升级目标设备
4. 选择目标的升级文件，开始OTA升级

## 升级方式说明
2. 客户可以选择基于jl_bt_ota的SDK开发，参考com.jieli.otasdk/tool/ota/。


| 库名 | 优势  | 劣势 | 备注 |
| --- | --- | --- | --- |
| jl_bt_ota | 1.固化OTA流程，不参与连接流程，方便客户改动<br> 2.不影响客户原因协议，可以部分功能接入 | 1. 需要客户实现连接流程和数据透传等接口 <br> 2. 接入相对复杂 | 建议使用 |


**设备通讯方式：** 默认是<strong style="color:#00008D">BLE</strong>，可选<strong style="color:#00008D">SPP</strong>，需要**固件**支持。

## OTA升级参数说明

**OTAManager**
```java
   val bluetoothOption = BluetoothOTAConfigure()
   //选择通讯方式
   bluetoothOption.priority = BluetoothOTAConfigure.PREFER_BLE
   //是否需要自定义回连方式(默认不需要，如需要自定义回连方式，需要客户自行实现)
   bluetoothOption.isUseReconnect = !JL_Constant.NEED_CUSTOM_RECONNECT_WAY
   //是否启用设备认证流程(与固件工程师确认)
   bluetoothOption.isUseAuthDevice = JL_Constant.IS_NEED_DEVICE_AUTH
   //设置BLE的MTU
   bluetoothOption.mtu = BluetoothConstant.BLE_MTU_MIN
   //是否需要改变BLE的MTU
   bluetoothOption.isNeedChangeMtu = false
   //是否启用杰理服务器(暂时不支持)
   bluetoothOption.isUseJLServer = false
   //是否需要调整BLE的MTU大小(默认不调整MTU，如果需要调整，请配合mtu属性设置)
   bluetoothOption.isNeedChangeMtu = false
   //配置OTA参数
   configure(bluetoothOption)
```

## Logcat开关说明

1. 开关LOG 可以使用JL_Log.setIsLog(boolean bl)设置
2. 保存LOG到本地 前提是Log已打开，并调用JL_Log.setIsSaveLogFile(boolean bl)设置
  * 若开启保存，退出应用前记得关闭保存Log文件
  * Log保存位置：
    * 如果手机系统是Android 10.0+，放到./Android/data/com.jieli.otasdk/files/com.jieli.otasdk/logcat/
    * 如果手机系统是Android 10.0以下，放到/com.jieli.otasdk/logcat/
    
## 版本渠道说明

1. Debug版本默认开启打印，可以选择测试配置。
  * 是否启用设备认证(默认开启)
  * 是否HID设备(默认关闭，回连方式有变化，因为HID设备系统会主动回连)
  *  是否自定义回连方式(默认关闭，如需要自定义回连方式，需要客户自行实现)
2. Release版本默认关闭打印，不显示测试配置。
