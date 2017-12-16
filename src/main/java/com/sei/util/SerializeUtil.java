package com.sei.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by vector on 16/6/14.
 */
public class SerializeUtil {

    public static String toBase64(Object obj){
        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
//        byte[] bytes = toByteArray(obj);
//        String str = Base64.encodeToString(bytes, 0, bytes.length,Base64.DEFAULT);
//        return str;
    }


    public static Object toObject(String base64, Class target){
//        byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
//        return ViewUtil.toObject(bytes);
        return JSON.parseObject(base64, target);
    }

    public static List toObjects(String base64, Class target){
//        byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
//        return ViewUtil.toObject(bytes);
        return JSON.parseArray(base64, target);
    }


    public static byte[] toByteArray (Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
            oos.close();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bytes;
    }
    public static Object toObject (byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream (bytes);
            ObjectInputStream ois = new ObjectInputStream (bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    public static String getAbbr(String name){
        String[] words = name.split("\\.");
        if(words.length == 0)
            return name;
        String result = "";
        for(int i = 0; i < words.length; i++){
            result += (""+words[i].charAt(0));
        }
        return result;
    }
}
