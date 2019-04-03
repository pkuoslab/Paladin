package com.sei.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import com.sei.util.CommonUtil;

public class CmdUtil {
    public static Process p = null;
    public static List<String> cmdList = Arrays.asList("npm","start","run");
    public static ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
    public interface ProcessState {
        int RUNNING = 1;
        int STOPPED = 0;
        int UNEXIST = -1;
    }

    /*
     * 返回ProcessState
     */
    static public int startProcess() {
        processBuilder.directory(new File("./remote-gleaning/"));
        if(p == null) {
            CommonUtil.log("start process for the first time...");
            try {
                p = processBuilder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (p == null) {
                return ProcessState.UNEXIST;
            }
            if(p.isAlive()) {
                return ProcessState.RUNNING;
            } else {
                return ProcessState.STOPPED;
            }
        } else {
            if(p.isAlive()) {
                CommonUtil.log("process is running, no need to restart");
                return ProcessState.RUNNING;
            } else {
                CommonUtil.log("process is stopped, try to restart");
                try {
                    p.destroy();
                    p = processBuilder.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (p == null) {
                    return ProcessState.UNEXIST;
                }
                if(p.isAlive()) {
                    return ProcessState.RUNNING;
                } else {
                    return ProcessState.STOPPED;
                }
            }
        }
    }

    static public int stopProcess() {
        if (p == null) {
            CommonUtil.log("process not exist");
            return ProcessState.UNEXIST;
        } else {
            int cnt = 3;    //尝试三次
            while(p.isAlive()) {
                p.destroyForcibly();
                CommonUtil.sleep(1000);
                cnt--;
                if(cnt<=0) {
                    CommonUtil.log("stop failed.");
                    return ProcessState.RUNNING;
                }
            }
            CommonUtil.log("process has stopped, no need to stop");
            return ProcessState.STOPPED;
        }
    }

    static public int restartProcess() {
        if(p == null) {
            CommonUtil.log("start process for the first time...");
            try {
                p = processBuilder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (p == null) {
                return ProcessState.UNEXIST;
            }
            if(p.isAlive()) {
                return ProcessState.RUNNING;
            } else {
                return ProcessState.STOPPED;
            }
        } else {
            if(p.isAlive()) {
                CommonUtil.log("restarting process");
                if(stopProcess() == ProcessState.RUNNING) {
                    CommonUtil.log("Pay attention!!!! restart failed");
                    return ProcessState.RUNNING;
                }
                return startProcess();
            } else {
                CommonUtil.log("process is stopped, try to restart");
                try {
                    p.destroy();
                    p = processBuilder.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (p == null) {
                    return ProcessState.UNEXIST;
                }
                if(p.isAlive()) {
                    return ProcessState.RUNNING;
                } else {
                    return ProcessState.STOPPED;
                }
            }
        }
    }
}
