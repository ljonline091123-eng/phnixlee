package com.zhaocai.business.sdk.util.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.HashMap;
import java.util.Map;


public class HttpMsg  {
	private String url;
	private String result;
	private String params;
	private int statusCode;
	private CloseableHttpResponse response;
	private HttpEntity entityRes;
	private Map<Integer, String> statusMsg = new HashMap<Integer, String>() {
		private static final long serialVersionUID = -8716277389366587533L;
		{
			put(200, "（成功）");
			put(201, "（已创建）");
			put(202, "（已接受）");
			put(203, "（非授权信息）");
			put(204, "（无内容）");
			put(205, "（重置内容）");
			put(206, "（部分内容）");
			put(300, "（多种选择）");
			put(301, "（永久移动）");
			put(302, "（临时移动）");
			put(303, "（查看其他位置）");
			put(304, "（未修改）");
			put(305, "（使用代理）");
			put(307, "（临时重定向）");
			put(400, "（错误请求）");
			put(401, "（未授权）");
			put(403, "（禁止）");
			put(404, "（未找到）");
			put(405, "（方法禁用）");
			put(406, "（不接受）");
			put(407, "（需要代理授权）");
			put(408, "（请求超时）");
			put(409, "（冲突）");
			put(410, "（已删除）");
			put(411, "（需要有效长度）");
			put(412, "（未满足前提条件）");
			put(413, "（请求实体过大）");
			put(414, "（请求的 URI 过长）");
			put(415, "（不支持的媒体类型）");
			put(416, "（请求范围不符合要求）");
			put(417, "（未满足期望值）");
			put(500, "（服务器内部错误）");
			put(501, "（尚未实施）");
			put(502, "（错误网关）");
			put(503, "（服务不可用）");
			put(504, "（网关超时）");
			put(505, "（HTTP 版本不受支持）");
			put(997, "（无返回信息）");
			put(998, "（编码异常）");
			put(999, "（地址不存在）");
		}
	};
	/**
	 * 得到响应数据
	 * @return result
	 */
	public String getResult() {
		return result;
	}
	/**
	 * 设置响应数据
	 * @param result 要设置的 result
	 */
	public void setResult(String result) {
		this.result = result;
	}
	/**
	 * 得到请求参数
	 * @return params
	 */
	public String getParams() {
		return params;
	}
	/**
	 * 设置请求参数
	 * @param params 要设置的 params
	 */
	public void setParams(String params) {
		this.params = params;
	}
	/**
	 * 得到响应状态码
	 * @return statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}
	/**
	 * 设置响应状态码
	 * @param statusCode 要设置的 statusCode
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	/**
	 * 得到状态码对应的信息
	 * @param statusCode
	 * @return
	 */
	public String getStatusMsg(int statusCode) {
		String s= statusMsg.get(statusCode);
		if(s==null) {
			return "（未知错误）未知的错误代码:"+statusCode;
		}
		return s;
	}
	/**
	 * 得到请求的URL
	 * @return url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * 设置请求的URL
	 * @param url 要设置的 url
	 */
	public void setUrl(String url) {
		this.url = url;
	}


    public CloseableHttpResponse getResponse() {
        return response;
    }

    public void setResponse(CloseableHttpResponse response) {
        this.response = response;
    }

    public HttpEntity getEntityRes() {
        return entityRes;
    }

    public void setEntityRes(HttpEntity entityRes) {
        this.entityRes = entityRes;
    }

    public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("url:").append(url);
		sb.append(";statusCode:").append(statusCode);
		sb.append(";msg:").append(this.getStatusMsg(statusCode));
		return sb.toString();
	}
}
