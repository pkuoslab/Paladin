# Paladin

## Introduction
This project implements a tool for automated generation of test cases for Android apps.
>Y. Ma, Y. Huang, Z.Hu, X.Xiao, X.Liu Paladin: Automated Generation of Reproducible Test Cases for Android Apps. In *HotMobile*, 2019


Paladin consists of: 
* packed `paladin.jar`
* a configuration file `config.json` 
* two andriod apk:  `uiautomator.apk` and `uiautomator-androidTest.apk`. 


Paladin depends on the java environment and the Android SDK. It has been tested in ubuntu 16.04, mac and Windows 10 environment.

## Setup

### Step 1. Compile paladin.jar

```shell
git clone https://github.com/pkuoslab/Paladin.git
cd paladin
gradle fatjar
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
- stop testing before replay
- or use `java -jar paladin.jar -p` to replay
- request `http://127.0.0.1:5700/replay?serial=xxx&nodes=activity_id` to generate a test case that reaches a specific page. The target page is identified by a unique id. To find all available id and corresponding screenshot, you can save screenshot while testing by setting `config.json`.  
- you can also request `http://127.0.0.1:5700/replay?serial=xxx&nodes=all` to generate a group of test cases that cover all available pages in the graph.
 