package com.jlm.homework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jlm.homework.dto.RongMsgResult;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.codec.digest.DigestUtils.sha1;

public class RongCloudUtil {
    private static final String App_Key = "c9kqb3rdcfkyj";
    private static final String App_Secret = "A8Me8tGPonV2k";
    private static final String AppSecret = "sk-AQ1wNXR2aTlkc3BjdG40c7E3g_yWJ1vtloFr";
    private static final String TIMESTAMP = "RC-Timestamp";
    private static final String userUrl = "https://api.rong-api.com/user/info.json";
    private static final String tokenUrl = "https://api.rong-api.com/user/getToken.json";
    private static final String publishMsgUrl="https://api.rong-api.com/message/private/publish.json";
    private static final String historyMsgUrl="https://api.rong-api.com/message/history.json";


    public  static  String getToken( String userId, String userHead, String userName) {
        StringBuffer res = new StringBuffer();
        HttpPost httpPost = new HttpPost(tokenUrl);
        HttpClient httpClient = getHttpClient(httpPost);
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
        nameValuePair.add(new BasicNameValuePair("name", userName));//名称（例如使用这个功能的‘张三’）
        nameValuePair.add(new BasicNameValuePair("userId", userId));// 用户id（根据自己的项目，自己生成一个串就行，UUID就行）
        nameValuePair.add(new BasicNameValuePair("portraitUri", userHead));//头像(存储头像的路径)
        HttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair, "utf-8"));
            httpResponse = httpClient.execute(httpPost);
            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line = null;
            while ((line = br.readLine()) != null) {
                res.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
         System.out.println("res=" + res.toString());
        String  token = "";
        if(res.indexOf("token")>0){
            JSONObject jsonObject = JSON.parseObject(res.toString());
            token =jsonObject.getString("token");
        }

        //Logger.i(userRespone.getCode()+"");
        //  System.out.println(jsonObject.getString("token"));
        return token;
    }

    @NotNull
    private static HttpClient getHttpClient(HttpPost httpPost) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);//时间戳，从 1970 年 1 月 1 日 0 点 0 分 0 秒开始到现在的秒数。
        String nonce = String.valueOf(Math.floor(Math.random() * 1000000));//随机数，无长度限制。
        String signature = sha1(App_Secret + nonce + timestamp).toString();//数据签名。
        //Logger.i(Signature);
        HttpClient httpClient = new DefaultHttpClient();

        httpPost.setHeader("App-Key", App_Key);
        httpPost.setHeader("Timestamp", timestamp);
        httpPost.setHeader("Nonce", nonce);
        httpPost.setHeader("Signature", signature);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        return httpClient;
    }

    public static RongMsgResult publishMsg(String fromUserId, String toUserId, String objectName, String content) {
        StringBuffer res = new StringBuffer();
        HttpPost httpPost = new HttpPost(publishMsgUrl);
        HttpClient httpClient = getHttpClient(httpPost);
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
        nameValuePair.add(new BasicNameValuePair("fromUserId", fromUserId));
        nameValuePair.add(new BasicNameValuePair("toUserId", toUserId));
        nameValuePair.add(new BasicNameValuePair("objectName", objectName));//类型
        nameValuePair.add(new BasicNameValuePair("content", content));//类型
        HttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair, "utf-8"));
            httpResponse = httpClient.execute(httpPost);
            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line = null;
            while ((line = br.readLine()) != null) {
                res.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSON.parseObject(res.toString());
        RongMsgResult  result = jsonObject.toJavaObject(RongMsgResult.class);
        return result;
    }
    public static JSONObject userInfo(String userId) {
        StringBuffer res = new StringBuffer();
        HttpPost httpPost = new HttpPost(userUrl);
        HttpClient httpClient = getHttpClient(httpPost);
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
        nameValuePair.add(new BasicNameValuePair("userId", userId));
        HttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair, "utf-8"));
            httpResponse = httpClient.execute(httpPost);
            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line = null;
            while ((line = br.readLine()) != null) {
                res.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSON.parseObject(res.toString());
        return jsonObject;
    }

    public static JSONObject getHistoryMsg(String date) {
        StringBuffer res = new StringBuffer();
        HttpPost httpPost = new HttpPost(historyMsgUrl);
        HttpClient httpClient = getHttpClient(httpPost);
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
        nameValuePair.add(new BasicNameValuePair("date", date));
        HttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair, "utf-8"));
            httpResponse = httpClient.execute(httpPost);
            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line = null;
            while ((line = br.readLine()) != null) {
                res.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSON.parseObject(res.toString());
        return jsonObject;
    }
}
