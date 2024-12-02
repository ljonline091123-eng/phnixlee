package com.zhaocai.business.sdk.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * @Description: HTTP工具类
 * @Auther: 樊增鑫
 * @Date: 2019-11-19 09:50
 * @Version: V1.0 RELEASE
 */
public class HttpUtils {

    /**
     * get请求
     *
     * @param url 请求地址
     * @return 响应结果
     */
    public static String sendGet(String url) {
        return sendGet(url, null, null);
    }

    /**
     * get请求
     *
     * @param url    请求地址
     * @param params 请求参数集合
     * @return 响应结果
     */
    public static String sendGet(String url, Map<String, String> params) {
        return sendGet(url, params, null);
    }

    /**
     * get请求
     *
     * @param url     请求地址
     * @param params  请求参数集合
     * @param headers 请求头集合
     * @return 响应结果
     */
    public static String sendGet(String url, Map<String, String> params, Map<String, String> headers) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String result = "";
        try {
            // 拼接参数
            if (params != null && params.size() > 0) {
                StringBuffer urlSB = new StringBuffer(url);
                boolean hasParams = url.indexOf("?") != -1;
                for (Map.Entry<String, String> it : params.entrySet()) {
                    if (!hasParams) {
                        urlSB.append("?");
                        hasParams = true;
                    } else {
                        urlSB.append("&");
                    }
                    urlSB.append(it.getKey()).append("=").append(URLEncoder.encode(it.getValue(), "UTF-8"));
                }
                url = urlSB.toString();
            }
            // 通过址默认配置创建一个httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpGet远程连接实例
            HttpGet httpGet = new HttpGet(url);
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> it : headers.entrySet()) {
                    httpGet.addHeader(it.getKey(), it.getValue());
                }
            }
            // 设置配置请求参数
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)// 连接主机服务超时时间
                    .setConnectionRequestTimeout(10000)// 请求超时时间
                    .setSocketTimeout(60000)// 数据读取超时时间
                    .build();
            // 为httpGet实例设置配置
            httpGet.setConfig(requestConfig);
            // 执行get请求得到返回对象
            response = httpClient.execute(httpGet);
            // 通过返回对象获取返回数据
            HttpEntity entity = response.getEntity();
            // 通过EntityUtils中的toString方法将结果转换为字符串
            result = EntityUtils.toString(entity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 发起POST请求
     *
     * @param url    请求url地址
     * @param params post请求参数
     * @return
     */
    public static String sendPost(String url, Map<String, Object> params) {
        return sendPost(url, params, null);
    }

    /**
     * 发起POST请求
     *
     * @param url    请求url地址
     * @param params post请求参数
     * @return
     */
//    public static String sendPost(String url, JSONObject params) throws IOException {
//        return sendPost(url, params, null);
//    }

    /**
     * 发起POST请求
     *
     * @param url     请求url地址
     * @param params  post请求参数
     * @param headers headers请求参数
     * @return
     */
    public static String sendPost(String url, Map<String, Object> params, Map<String, String> headers) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        String result = "";
        // 创建httpClient实例
        httpClient = HttpClients.createDefault();
        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(10000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> it : headers.entrySet()) {
                httpPost.addHeader(it.getKey(), it.getValue());
            }
        }
        // 封装post请求参数
        if (null != params && params.size() > 0) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            // 通过map集成entrySet方法获取entity
            Set<Map.Entry<String, Object>> entrySet = params.entrySet();
            // 循环遍历，获取迭代器
            Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> mapEntry = iterator.next();
                nvps.add(new BasicNameValuePair(mapEntry.getKey(), mapEntry.getValue().toString()));
            }
            // 为httpPost设置封装好的请求参数
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 发送POST 请求
     *
     * @param urlStr
     * @param data
     * @param header
     * @return
     */
    public static String sendPost(String urlStr, JSONObject data, Map<String, String> header) throws IOException {
        return sendPost(urlStr, data.toString(), header);
    }

    /**
     * 发送POST 请求
     *
     * @param urlStr
     * @param data
     * @param header
     * @return
     */
    public static String sendPost(String urlStr, String data, Map<String, String> header) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // 设置可输入
        connection.setDoInput(true);
        // 设置该连接是可以输出的
        connection.setDoOutput(true);
        // 设置请求方式
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        if (header != null && header.size() > 0) {
            for (String key : header.keySet()) {
                connection.setRequestProperty(key, header.get(key));
            }
        }
        PrintWriter pw = new PrintWriter(new BufferedOutputStream(connection.getOutputStream()));
        pw.write(data);
        pw.flush();
        pw.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        String line = null;
        StringBuilder result = new StringBuilder();
        // 读取数据
        while ((line = br.readLine()) != null) {
            result.append(line + "\n");
        }
        connection.disconnect();
        return result.toString();
    }

    /**
     * 向指定 URL 上传文件POST方法的请求
     *
     * @param url       发送请求的 URL
     * @param dataParam 数据参数
     * @return 所代表远程资源的响应结果, json数据
     */
    public static String post(String url, Map<String, Object> dataParam) {
        return post(url, dataParam, null);
    }

    /**
     * 向指定 URL 上传文件POST方法的请求
     *
     * @param url       发送请求的 URL
     * @param dataParam 数据参数
     * @param headers   header参数
     * @return 所代表远程资源的响应结果, json数据
     */
    public static String post(String url, Map<String, Object> dataParam, Map<String, String> headers) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectTimeout(120000).build();
        CloseableHttpResponse response = null;
        String result = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> it : headers.entrySet()) {
                    httpPost.addHeader(it.getKey(), it.getValue());
                }
            }
            MultipartEntityBuilder mEntityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            mEntityBuilder.setCharset(Charset.forName("UTF-8"));
            if (dataParam != null && dataParam.size() > 0) {
                for (Map.Entry<String, Object> entry : dataParam.entrySet()) {
                    if (entry.getValue() instanceof File) {
                        mEntityBuilder.addPart(entry.getKey(), new FileBody((File) entry.getValue()));
                    } else {
                        mEntityBuilder.addPart(entry.getKey(), new StringBody(entry.getValue().toString(), ContentType.APPLICATION_JSON));
                    }
                }
            }
            HttpEntity reqEntity = mEntityBuilder.build();
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity resEntity = response.getEntity();
                byte[] josn = EntityUtils.toByteArray(resEntity);
                result = new String(josn, "UTF-8");
                EntityUtils.consume(resEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpclient);
            HttpClientUtils.closeQuietly(response);
        }
        return result;
    }
}
