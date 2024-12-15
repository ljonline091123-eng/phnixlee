package com.zhaocai.business.sdk.util.httpclient;

import org.apache.http.client.config.RequestConfig;

import java.io.IOException;
import java.util.Map;


/**
 * httpClient 操作
 * 
 * @author ytlzb
 *
 */
public class UtilHttpClient {

	private static PooledHttpClientAdaptor hc = null;
	private static RequestConfig requestConfigStream=null;
	private static RequestConfig requestConfigStr=null;
	private static void init() {
		if (hc == null) {
			System.out.println("启动HttpClient的线程池...");
			hc = new PooledHttpClientAdaptor();
			requestConfigStream = RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(3000).setSocketTimeout(120000).build();
			requestConfigStr = RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(3000).setSocketTimeout(90000).build();
		}
	}

	/**
	 * post提交
	 * 
	 * @param url
	 * @param postStr
	 *            post提交的字符串
	 * @return
	 */
	public static HttpMsg post(String url, String postStr)  {
		init();
		return hc.doPost(url, postStr,requestConfigStr);
	}


    /**
     * post提交返回是流
     *
     * @param url
     * @param postStr
     *            post提交的字符串
     * @return
     */
    public static HttpMsg postForStream(String url, String postStr)  {
        init();
        return hc.doPostForStream(url, postStr,requestConfigStream);
    }


	public static HttpMsg post(String url, String postStr, RequestConfig requestCfg)  {
		init();
		return hc.doPost(url, postStr, requestCfg);
	}

	/**
	 * Get提交
	 * 
	 * @param url
	 * @throws IOException
	 */
	public static HttpMsg get(String url)  {
		init();
		return hc.doGet(url);
	}

	/**
	 * HttpClient的post方法
	 * 
	 * @param url
	 * @param params
	 *            参数map集合
	 * @return
	 * @throws IOException
	 */
	public static HttpMsg post(String url, Map<String, String> params)  {
		init();
		return hc.doPost(url, params);
	}

}
