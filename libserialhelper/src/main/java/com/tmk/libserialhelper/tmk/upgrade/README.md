# 说明
> 本库是Uart升级库


## 流程


## 调用方法
主要类是 **UartOtaManager** 这个类
1.调用 `UartOtaManager.getInstance(context,)` 获取一个实例
2.配置参数 `UartConfig`， 配置文件升级的 fileData，是否打印日志等
3.设置监听 `UartEventListener` ，监听升级进度、结果、错误等
4.调用`startOta` 方法开始升级