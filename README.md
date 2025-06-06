# Android-JL_OTA
The bluetooth OTA for Android

## 快速开始

为了帮助开发者快速接入杰理OTA方案，请开发前详细阅读SDK开发文档: [杰理OTA外接库开发文档(Android)](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/index.html)。



## 接入答疑

针对开发者反馈的常见问题进行统一答疑，开发者遇到问题时，可以先参考 [常见问题答疑](https://doc.zh-jieli.com/Apps/Android/ota/zh-cn/master/other/qa.html)。<br/>
如果还是无法解决问题，请提交issue，我们将尽快回复。



## 更新内容

1. 修复SPP 方式单备份 OTA 失败问题<br>
2. 增加 Android 14 的兼容处理<br>
3. 重构APP的UI框架



## 压缩包文件结构说明

```tex
apk  --- 测试APK
 ├── JLOTA_V1.8.0_10800-debug.apk
code --- 演示程序源码
 ├── 参考Demo源码工程
doc  --- 开发文档
 ├── 杰理OTA外接库(Android)开发文档链接
libs --- 核心库
 └── jl_bt_ota_V1.10.0-release
```



## 使用说明

1. 打开APP(初次打开应用，需要授予对应权限)<br>

2. 拷贝升级文件到手机固定的存放位置 `手机根目录/Android/data/com.jieli.otasdk/files/upgrade/`<br>

3. 连接升级目标设备<br>

4. 选择目标的升级文件，开始OTA升级

   

## 升级方式说明
1. 客户可以选择基于jl_bt_ota的SDK开发，参考 ``com.jieli.otasdk/tool/ota/``。


| 库名 | 优势  | 劣势 | 备注 |
| --- | --- | --- | --- |
| jl_bt_ota | 1. 固化OTA流程，不参与连接流程，方便客户改动<br> 2.不影响客户原因协议，可以部分功能接入 | 1. 需要客户实现连接流程和数据透传等接口 <br />2. 接入相对复杂 | 建议使用 |

**设备通讯方式：** 默认是<strong style="color:#00008D">BLE</strong>，可选<strong style="color:#00008D">SPP</strong>，需要**固件**支持。



## OTA升级参数说明

**OTAManager**
```java
   val bluetoothOption = BluetoothOTAConfigure()
   //选择通讯方式
   bluetoothOption.priority = BluetoothOTAConfigure.PREFER_BLE
   //是否需要自定义回连方式(默认是false，使用库内回连机制，如需要自定义回连方式，设置为true, 由客户自行实现回连机制)
   bluetoothOption.isUseReconnect = !JL_Constant.NEED_CUSTOM_RECONNECT_WAY
   //是否启用设备认证流程(与固件工程师确认)
   bluetoothOption.isUseAuthDevice = JL_Constant.IS_NEED_DEVICE_AUTH
   //设置BLE的MTU(已过时，不建议使用)
   bluetoothOption.mtu = BluetoothConstant.BLE_MTU_MIN
   //是否需要改变BLE的MTU(已过时，不建议使用)
   bluetoothOption.isNeedChangeMtu = false
   //是否启用杰理服务器(暂时不支持)
   bluetoothOption.isUseJLServer = false
   //配置OTA参数
   configure(bluetoothOption)
```



<strong style="color:#dd2233">注意事项</strong>

1. 公版固件SDK默认是打开【**设备认证**】功能的



## Logcat开关说明

1. 代码设置

   ```java
   //log配置
   //islog      --- 是否输出打印，建议是开发时打开，发布时关闭
   JL_Log.setIsLog(BuildConfig.DEBUG);
   //log文件配置
   //context    --- 上下文，建议是getApplicationContext()
   //isSaveFile --- 是否保存log文件，建议是开发时打开，发布时关闭
   JL_Log.setIsSaveLogFile(context, BuildConfig.DEBUG);
   ```

​		**注意事项**

```tex
	1.  建议在Application中设置打印输出
	1.  debug版本默认开启打印, release版本默认关闭打印
	1.  客户可以在demo工程配置是否开启debug调试
```



2. 打印文件

  * 打印文件格式: ota_log_app_[timestamp].txt

    * timestamp: 时间戳

    ```tex
    例如: ota_log_app_20220330093020.432.txt ==> OTA外接库打印文件, 创建时间: 2022/03/30 09:30:20
    ```

  * Log文件保存位置：`手机根目录/Android/data/[包名]/files/logcat/`

    * 包名: 应用包名， 比如: `com.jieli.otasdk`

    ```tex
    举例: Android/data/com.jieli.otasdk/files/logcat/
    ```



## 异常处理步骤

<strong style="color:#ee2233">前提： 出现异常情况后, 退出APP</strong>

1. **简单描述问题现象 (必要)**

2. **提供最接近时间戳的log文件 (必要)**

3. 简要描述发生异常现象的时间段

4. 提供现象的截图或者视频
