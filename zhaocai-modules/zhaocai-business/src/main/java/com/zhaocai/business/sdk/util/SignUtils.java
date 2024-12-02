package com.zhaocai.business.sdk.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignUtils {

	/**
	 * 创建签名
	 * @param appSecret
	 *            app秘钥
	 * @param method
	 *            请求方法名 从请求头获取 一般为大写 比如 GET POST
	 * @param uri
	 *            url端口后面所有字符
	 * @param dateStr
	 *            日期字符串 毫秒时间戳
	 * @param body
	 *            contentType=application/json 的时候会 取body 其他情况不取
	 * @param algorithm
	 *            算法类型 例如 SHA SHA256 MD5
	 */
	public static String generatorSign(String appSecret, String method, String uri, String dateStr, String body,
			String algorithm) throws NoSuchAlgorithmException {
		StringBuilder preSignBuilder = new StringBuilder();
		// app_secret + HttpMethod + URI + Date + HttpBody
		preSignBuilder.append(appSecret).append(method).append(uri).append(dateStr).append(body == null ? "" : body);
		MessageDigest md = MessageDigest.getInstance(algorithm);
		byte[] digest = md.digest(preSignBuilder.toString().getBytes());
		// 字节流转16进制字符串
		StringBuilder sign = new StringBuilder();
		for (byte b : digest) {
			String hex = Integer.toHexString(b & 0xFF);
			if (hex.length() == 1) {
				sign.append("0");
			}
			sign.append(hex);
		}
		return sign.toString();
	}
}
