# Paladin

## 2019.4.2 Update — Webview 

We have updated paladin with a new feature, which will enable Paladin to retrieve web view content. Now you just need to add one more field in the `config.json`  then you will be able to experience Paladin's new feature. Below is the more specific introduction on how to use it.

### System Requirement

- Unfortunately, this feature has some system request. Because for safety reasons, app developers need to invoke static method `setWebConTentsDebuggingEnabled(true)` in `WebView` class so that others can get the web view content in the app from outside of the app. You can see more information on <https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews?hl=zh-cn>. As far as we know, typically seldom apps have invoked this method. However, maybe for more convenience debugging, some systems have done this automatically. So under this kind of system, we can get the web view content although app developers do not do this setting.

- If you want to experience this feature, we strongly recommend you to use [genymotion android emulator](https://www.genymotion.com/) for your testing. It is one of the systems we know enables us to retrieve web view content freely.
- Another alternative system for you is [CyanogenMOD](https://www.cyanogenmods.org/). As far as we know, it has been renamed as Lineage OS, but Lineage OS does not support what we have said before while CyanogenMOD system does. You can get CyanogenMOD system for some brands of mobile phones by Google. But, to be honest, it's really a hard way because the official website has stopped updating it.

### Setup and Testing

- Add the field `"WEBVIEW" : true` in `config.json`. Pay attention that the `"SCREENSHOT"` field must be set `true`, so that you can see the web view result after testing. The modified  `config.json` is like this:

  ```json
  {
      "ADB_PATH": "/Users/ycx/Documents/android-sdk-macosx/platform-tools/",
      "DEFAULT_PORT": 5700,
      "BACKEND": "UIAutomator",
      "SCREENSHOT" : true,
      "WEBVIEW" : true,
      "DEVICES": [
          {
              "IP": "127.0.0.1",
              "SERIAL": "192.168.56.101:5555",
              "PORT": 6161
          }
      ],
      "PACKAGE": "com.zuzuChe"
  }
  ```

- Use `java -jar paladin.jar`  to start paladin.
- The other commands remain the same.

### Check Testing Result

- The testing result is under the folder ./output/PACKAGE_NAME/. Each page's result Paladin has found includes two files — an image file and a json file.
- The image file is the page's screenshot.
- The json file, which we called view tree, is the content of the page. It is a tree structure which not only shows the hierarchy of the elements in this page but also shows the details of these elements. The details contain the element's width, height, text, tag and so on.
- For web view content, which is typically written in HTML language, we also include them in view tree, which is not implemented before this update. We conceal the difference between web view content and normal page content. Because it is more in line with people's habits of using the app since people seldom know the existence of the web view. The difference between web view element and normal element in view tree is that web view element's `WebContent` field is `true`.
- If you want to check if a page has the web view content, the view tree has a field called `hasWebview`. It will be set true once Paladin finds there is web view content in this page.


## Introduction
This project implements a tool for automated generation of test cases for Android apps.
>Y. Ma, Y. Huang, Z.Hu, X.Xiao, X.Liu Paladin: Automated Generation of Reproducible Test Cases for Android Apps. In *HotMobile*, 2019


Paladin consists of: 
* packed `paladin.jar`
* a configuration file `config.json` 
* two andriod apk:  `uiautomator.apk` and `uiautomator-androidTest.apk`. 

Requirements: jdk1.8, Android SDK(v26.1.1+), gradle(v4.10.2+)  
Paladin has been tested in ubuntu 16.04, mac and Windows 10 environment.

## Setup

### Step 1. Compile paladin.jar

```shell
git clone https://github.com/pkuoslab/Paladin.git
cd Paladin
./gradlew fatjar
mv build/libs/paladin-1.0.jar paladin.jar
mv build/libs/config.json config.json
```

### Step 2. Test phone setup

- Install  `uiautomator.apk` and `uiautomator-androidTest.apk`
- Install the app you want to test on your phone

### Step 3. Modify config.json

```json
{
    "ADB_PATH": "D:/android/SDK/platform-tools/",
    "DEFAULT_PORT": 5700,
    "BACKEND": "UIAutomator",
    "SCREENSHOT" : false,
    "DEVICES": [
        {
            "IP": "127.0.0.1",
            "SERIAL": "d6b534d7",
            "PORT": 6161
        }
    ],
    "PACKAGE": "com.tencent.mm"
}
```

- **"ADB_PATH"**: The path of ADB (need to install Android SDK)
- "DEFAULT_PORT": Port to interact with paladin control terminal
- "BACKEND": Choose`"UIAutomator"`
- "SCREENSHOT": set true to save screenshot, the screenshot will be saved under ./output/PACKAGE_NAME/
- "DEVICES": A list of your test phone
  - "IP": ip of test phone, `"127.0.0.1"` is recommended.
  - **"SERIAL"**: The serial number of your test phone. Use `adb devices` to list devices availabe.
  - "PORT": If you use more than one test phone, this field should be mutually different. 
- **"PACKAGE"**: Package name of the app you want to test.

**Bold Word** indicates the field you need to change when first using Paladin.

## Testing

### Start testing

- Install your testing app on your test phone, modify `config.json`.
- Use `java -jar paladin.jar`  to start paladin.

### Save graph

request `http://127.0.0.1:5700/save` to save the graph of your testing app. The graph file is under the path where you run `paladin.jar`. 

### List activities

request `http://127.0.0.1:5700/list` to see how many activities paladin has found.

### Stop testing

request `http://127.0.0.1:5700/stop?serial=xxx` to stop paladin. You can restart your work by using `java -jar paladin.jar`. Paladin will start from where you saved the graph.

### Replay
The saved graph file is used to replay
- use `Ctrl+C` to stop exploration
- use `java -jar paladin.jar -p` to start replay
- request `http://127.0.0.1:5700/replay?serial=xxx&nodes=activity_id` to generate a test case that reaches a specific page. The target page is identified by a unique id. To find all available id and corresponding screenshot, you can save screenshot while testing by setting `config.json`.  
- you can also request `http://127.0.0.1:5700/replay?serial=xxx&nodes=all` to generate a group of test cases that cover all available pages in the graph.


