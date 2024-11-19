package com.zhaocai.business.demo.app;

import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.util.HttpUtils;
import com.zhaocai.business.sdk.util.SignUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * 接口发送类
 * 
 * @author ytlzb
 *
 */
public class Sender {
	/**
	 * 服务部署地址
	 */
	private static final String SERVER_URL = "http://uat.yozopoc.com:9010/apiserver";
	/**
	 * 分配的应用代码
	 */
	private static final String AppCode = "ahtsabigJr0q3oa9Bs";
	/**
	 * 分配的应用秘钥，请不要通过参数传递
	 */
	private static final String AppSecret = "GeIGZSkQRq2ISjCc3YFCqt3BD3";

	/**
	 * Http请求方法，POST
	 */
	private static final String HTTP_METHOD_POST = "POST";
	/**
	 * Http请求方法，GET
	 */
	private static final String HTTP_METHOD_GET = "GET";
	/**
	 * url拼接时的转换类型名称
	 */
	private static final String URL_CONVERT_TYPE_PREFIX = "convertType";

	/**
	 * 签名算法，该算法对应的Yozo-Sign 前拼接 YOZO-1
	 */
	private static final String SIGN_METHOD_SHA = "SHA";
	/**
	 * 签名算法，该算法对应的Yozo-Sign 前拼接 YOZO-2
	 */
	private static final String SIGN_METHOD_SHA256 = "SHA256";
	/**
	 * 签名算法，该算法对应的Yozo-Sign 前拼接 YOZO-3
	 */
	private static final String SIGN_METHOD_MD5 = "MD5";

	


	/**
	 * 带文件流的请求
	 * 
	 * @param url
	 *            接口的URL,请传递常量，便于以后根绝业务需要修改统一的接口地址
	 * @param convertType
	 *            转换类型，请传递常量，便于以后升级统一处理
	 * @param requestBody
	 * @return 响应结果
	 * @throws NoSuchAlgorithmException
	 */
	public static String post(String url, String convertType, Map<String, Object> requestBody)
			throws NoSuchAlgorithmException {
	//如果是编辑，即便传错了转换类型，也强制修改为编辑
		if(EditParams.URL_EDIT.equals(url)) {
			convertType=EditParams.CONVERT_TYPE_EDIT;
		}
		//请求的url
		String sendUrl=buildUrl(url, convertType);
		//时间戳
		String timeStamp = nowTimeMillis();
		// 计算签名,带文件上传的接口，仅对时间戳和url参数进行签名
		String sign = SignUtils.generatorSign(AppSecret, HTTP_METHOD_POST, sendUrl, timeStamp, "", SIGN_METHOD_SHA);
		// 封装签名参数
		Map<String, String> header = new HashMap<String, String>();
		//日期
		header.put("Yozo-Date", timeStamp);
		//签名数据
		header.put("Yozo-Sign", "YOZO-1:" + AppCode + ":" + sign);
		//请使用自己框架的日志输入，不建议使用System.out
		System.out.println("请求URL：" +sendUrl);
		System.out.println("Header请求参数：");
		System.out.println(header);
		System.out.println("Body请求参数：");
		System.out.println(requestBody);
		//得到完整的url
		sendUrl=SERVER_URL+sendUrl;
		// 发起网络请求
		String response = HttpUtils.post(sendUrl, requestBody, header);
		return response;
	}

	/**
	 * 不带文件流的上传
	 * 
	 * @param url 接口的URL,请传递常量，便于以后根绝业务需要修改统一的接口地址
	 * @param convertType
	 *            转换类型，请传递常量，便于以后升级统一处理
	 * @param requestBody json格式的字符串
	 * @return 响应结果
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String post(String url, String convertType,String requestBody) throws NoSuchAlgorithmException, IOException {
		//请求的url
		String sendUrl=buildUrl(url, convertType);
		//时间戳
		String timeStamp = nowTimeMillis();
		// 计算签名,对内容及body体进行签名
		String sign = SignUtils.generatorSign(AppSecret, HTTP_METHOD_POST, sendUrl, timeStamp, requestBody,
				SIGN_METHOD_SHA);
		// 封装签名参数
		Map<String, String> header = new HashMap<String, String>();
		//日期
		header.put("Yozo-Date", timeStamp);
		//签名数据
		header.put("Yozo-Sign", "YOZO-1:" + AppCode + ":" + sign);
		
		System.out.println("请求URL：" +sendUrl);
		System.out.println("Header请求参数：");
		System.out.println(header);
		System.out.println("Body请求参数：");
		System.out.println(requestBody);
		//得到完整的url
		sendUrl=SERVER_URL+sendUrl;
		// 发起网络请求
		String response = HttpUtils.sendPost(sendUrl, requestBody, header);
		return response;
	}

	/**
	 * 构建请求的url
	 * 
	 * @param url
	 * @param convertType
	 * @return
	 */
	private static String buildUrl(String url, String convertType) {
		if (convertType == null || "".equals(convertType.trim())) {
			return url;
		}
		if (url == null) {
			return null;
		}
		url = url.trim();
		if ("".equals(url)) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		sb.append(url);
		if (url.indexOf("?") > 0) {
			sb.append("&");
		} else {
			sb.append("?");
		}
		sb.append(URL_CONVERT_TYPE_PREFIX).append("=").append(convertType);
		return sb.toString();
	}

	/**
	 * 当前服务器时间戳
	 * 
	 * @return 字符串格式的时间戳
	 */
	private static String nowTimeMillis() {
		return String.valueOf(System.currentTimeMillis());
	}
}
