# Paladin文档

---
## 1.使用
* 1.编译：使用gradle(https://gradle.org/)
在项目目录下使用命令行: gradle fatjar，即可在build/libs下编译成jar。
* 2.部署，将uiautomator.apk和uiautomator-androidTest.apk安装于测试手机，设置config.json中的adb路径与测试apk包名，在pc端运行: `java -jar paladin.jar`即可开始遍历。
* 3.保存测试图，发送http://127.0.0.1:5700/save即可保存当前遍历得到的图。

## 2.主要文件
* 1. 程序以server/Control.java为入口，并通过服务器与外界交互
* 2. server/component/Scheduler.java负责作出决策
* 3. agent/Device.java负责执行决策，并将结果反馈给Scheduler.java
* 4. modules/*.java为不同的决策模块
* 5. bean/Collection/Graph内的文件为遍历图的抽象结构, bean/View/内的文件为抽象UI结构



