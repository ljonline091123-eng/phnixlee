package com.zhaocai.business.sdk.util.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * HttpClient的操作
 * 
 * @author ytlzb
 * 
 */
public class HttpClient {
	// 连接超时时间，默认10秒
	private int socketTimeout = 30000;
	// 传输超时时间，默认30秒
	private int connectTimeout = 60000;
	private boolean hasInit = false;
	private CloseableHttpClient httpClient;
	RequestConfig requestConfig;

	/**
	 * 初始化证书信息
	 * 
	 * @param certLocalPath
	 *            证书地址
	 * @param password
	 *            证书密码，对应的是商户号
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws CertificateException
	 */
	private void init(String certLocalPath, String password) throws IOException, KeyStoreException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyManagementException, CertificateException {
		if (certLocalPath != null) {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			FileInputStream instream = new FileInputStream(new File(certLocalPath));// 加载本地的证书进行https加密传输
			try {
				keyStore.load(instream, password.toCharArray());// 设置证书密码
			} catch (CertificateException e) {
				// 抛出错误
				// e.printStackTrace();
				throw e;
			} catch (NoSuchAlgorithmException e) {
				// 抛出错误
				// e.printStackTrace();
				throw e;
			} finally {
				instream.close();
			}
			// Trust own CA and all self-signed certs
			SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, password.toCharArray()).build();
			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
					SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			// 初始化
			httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

		} else {
			// 初始化
			httpClient = HttpClients.createDefault();
		}
		// 根据默认超时限制初始化requestConfig
		requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();
		hasInit = true;
	}

	/**
	 * 发送post数据
	 * 
	 * @param url
	 * @param postString
	 * @param certLocalPath 证书的路径
	 * @param password 证书的密码
	 * @param request的自定义配置
	 * @return
	 * @throws Exception
	 */
	public String sendPost(String url, String postString, String certLocalPath, String password,RequestConfig requestCfg) throws Exception {
		if (!hasInit) {
			init(certLocalPath, password);
		}
		String result = null;
		HttpPost httpPost = new HttpPost(url);

		// 得指明使用UTF-8编码，否则到API服务器XML的中文不能被成功识别
		StringEntity postEntity = new StringEntity(postString, "UTF-8");
		httpPost.addHeader("Content-Type", "text/xml");
		httpPost.setEntity(postEntity);
		// 设置请求器的配置
		httpPost.setConfig(requestCfg);
		try {
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			result = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			// e.printStackTrace();
			throw e;
		} finally {
			httpPost.abort();
		}
		return result;
	}
	/**
	 * 发送post数据
	 * 
	 * @param url
	 * @param postString
	 * @param certLocalPath 证书的路径
	 * @param password 证书的密码
	 * @return
	 * @throws Exception
	 */
	public String sendPost(String url, String postString, String certLocalPath, String password) throws Exception {
		if (!hasInit) {
			init(certLocalPath, password);
		}
		String result = null;
		HttpPost httpPost = new HttpPost(url);
		
		// 得指明使用UTF-8编码，否则到API服务器XML的中文不能被成功识别
		StringEntity postEntity = new StringEntity(postString, "UTF-8");
		httpPost.addHeader("Content-Type", "text/xml");
		httpPost.setEntity(postEntity);
		// 设置请求器的配置
		httpPost.setConfig(requestConfig);
		try {
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			result = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			// e.printStackTrace();
			throw e;
		} finally {
			httpPost.abort();
		}
		return result;
	}

	/**
	 * Get提交
	 * 
	 * @param url
	 * @throws Exception
	 */
	public String sendGet(String url) throws Exception {
		if (!hasInit) {
			init(null, null);
		}
		HttpGet httpget = new HttpGet(url);
		try {
			// 执行get请求.
			CloseableHttpResponse response = httpClient.execute(httpget);
			try {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				// 打印响应状态
				String reValue = EntityUtils.toString(entity);
				// reValue = new String(reValue.getBytes("ISO8859-1"), "UTF-8");
				return reValue;
			} finally {
				response.close();
			}
		} catch (Exception e) {
			// e.printStackTrace();
			throw e;
		} finally {
			try {
				httpget.abort();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	public String sendGet(String url,String code) throws Exception {
		if (!hasInit) {
			init(null, null);
		}
		HttpGet httpget = new HttpGet(url);
		try {
			// 执行get请求.
			CloseableHttpResponse response = httpClient.execute(httpget);
			try {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				// 打印响应状态
				String reValue = EntityUtils.toString(entity,code);
				// reValue = new String(reValue.getBytes("ISO8859-1"), "UTF-8");
				return reValue;
			} finally {
				response.close();
			}
		} catch (Exception e) {
			// e.printStackTrace();
			throw e;
		} finally {
			try {
				httpget.abort();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	/**
	 * 必须销毁httpClient
	 */
	public void close() {
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
