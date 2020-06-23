# Android-JL_OTA
The bluetooth OTA for Android

概述
------------
 ## 压缩包文件结构说明
  apk -- 测试APK
  code -- 演示程序源码
  doc -- 开发文档
  libs -- 核心库
  
  
 ## 使用说明
 0. 打开APP，授予权限
 1. 拷贝升级文件()到手机固定的存放位置(如果是Android 10.0+，放到./Android/data/com.jieli.otasdk/files/com.jieli.otasdk/upgrade/；
 如果是Android 10.0以下，放到/com.jieli.otasdk/upgrade/)
 2.连接设备
 3. 开始OTA升级。

 
 升级方式说明
 1. 如果产品是基于RCSP协议开发的，直接参考demo。走的是jl_bluetooth_ac692x库的流程。
 2. 如果产品不是基于RCSP协议，用的是自身协议通讯，可以用OTA外接库的流程。同样参考demo。走的是jl_bt_ota库的流程。
 
 
 设备通讯方式：默认是BLE，可选SPP，需要固件支持。
 
 
 ## Logcat开关说明
 1. 开关LOG 可以使用JL_Log.setIsLog(boolean bl)设置
 2. 保存LOG到本地 前提是Log已打开，并调用JL_Log.setIsSaveLogFile(boolean bl)设置。
	2.1 若开启保存，退出应用前记得关闭保存Log文件。
	2.2 Log保存位置：如果是Android 10.0+，放到./Android/data/com.jieli.otasdk/files/com.jieli.otasdk/logcat/；如果是Android 10.0以下，放到/com.jieli.otasdk/logcat/