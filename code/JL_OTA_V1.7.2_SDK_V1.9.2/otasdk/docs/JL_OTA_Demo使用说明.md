# JL_OTA_Demo使用说明

# 使用说明
1. 打开APP(初次打开应用，需要授予对应权限)
2. 拷贝升级文件到手机固定的存放位置<br>
  * 如果手机系统是Android 10.0+，放到./Android/data/com.jieli.otasdk/files/com.jieli.otasdk/upgrade/<br>
  * 如果手机系统是Android 10.0以下，放到/com.jieli.otasdk/upgrade/
3. 连接升级目标设备
4. 选择目标的升级文件，开始OTA升级

# 升级方式说明
1. 客户可以选择基于jl_bluetooth_rcsp的SDK开发，参考com.jieli.otasdk/tool/jl_ota/。<strong style="color:#D80000">不建议使用，已弃更。</strong>
2. 客户可以选择基于jl_bt_ota的SDK开发，参考com.jieli.otasdk/tool/other_sdk_ota/。<strong style="color:#D80000">建议使用</strong>


| 库名 | 优势  | 劣势 | 备注 |
| --- | --- | --- | --- |
| jl_bluetooth_rcsp | 1.有完整的连接流程和OTA流程<br>2.接入简单  | 1.连接流程固定，不方便改动 <br>2.不方便接入其他协议通讯 | 不建议使用 |
| jl_bt_ota | 1.固化OTA流程，不参与连接流程，方便客户改动<br> 2.不影响客户原因协议，可以部分功能接入 | 1. 需要客户实现连接流程和数据透传等接口 <br> 2. 接入相对复杂 | 建议使用 |


**设备通讯方式：** 默认是<strong style="color:#00008D">BLE</strong>，可选<strong style="color:#00008D">SPP</strong>，需要**固件**支持。

# OTA升级参数说明

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
   //配置OTA参数
   configure(bluetoothOption)
```

# Logcat开关说明

1. 开关LOG 可以使用JL_Log.setIsLog(boolean bl)设置
2. 保存LOG到本地 前提是Log已打开，并调用JL_Log.setIsSaveLogFile(boolean bl)设置
  * 若开启保存，退出应用前记得关闭保存Log文件
  * Log保存位置：
    * 如果手机系统是Android 10.0+，放到./Android/data/com.jieli.otasdk/files/com.jieli.otasdk/logcat/
    * 如果手机系统是Android 10.0以下，放到/com.jieli.otasdk/logcat/

