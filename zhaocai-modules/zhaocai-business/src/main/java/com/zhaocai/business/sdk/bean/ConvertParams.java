package com.zhaocai.business.sdk.bean;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 转换参数对象
 * @author lizb
 *
 */
public class ConvertParams {
	/**
	 * 文件处理时需要的图片或文档上传URL,请定义常量，便于以后根绝业务需要修改统一的接口地址
	 */
	public static final String URL_CONVERT_FILE = "/pms/dcsUpload/upload";
	/**
	 * 文件转换URL,请定义常量，便于以后根绝业务需要修改统一的接口地址
	 */
	public static final String URL_CONVERT = "/pms/convert/upload";
	/**
	 * 文件处理时需要的图片或文档上传的类型，需要结合<b>URL_CONVERT_FILE</b>使用
	 */
	public static final String CONVERT_TYPE_CONVERT_FILE = "fill_office";
	/**
	 * office文档同类型转换处理
	 */
	public static final String CONVERT_TYPE_CONVERT_DOCUMENT = "convert_document_to_document";
	/**
	 * 文档转pdf
	 */
	public static final String CONVERT_TYPE_DOC_PDF = "convert_document_to_pdf";
	/**
	 * 文档转ofd
	 */
	public static final String CONVERT_TYPE_DOC_OFD = "convert_document_to_ofd";
	/**
	 * pdf文档同类型转换处理
	 */
	public static final String CONVERT_TYPE_CONVERT_PDF = "convert_pdf_to_pdf";
	/**
	 * ofd文档同类型转换处理
	 */
	public static final String CONVERT_TYPE_CONVERT_OFD = "convert_ofd_to_ofd";
	
	private Map<String, Object> requestBody;
	private JSONObject extraParam;
	private BookMark bookMark;
	public ConvertParams() {
		 requestBody = new HashMap<>();
		 extraParam=new JSONObject();
	}
	/**
	 * 设置转换文件，仅适用于/pms/convert/upload
	 * @param filePath 文件路径
	 * @throws IOException
	 */
	public void setFilePath(String filePath) throws IOException {
		if(filePath==null || "".equals(filePath.trim())) {
			throw new IOException("设置转换的服务器文件地址为空");
		}
		requestBody.put("file", new File(filePath));
	}
	/**
	 * 设置文件路径，仅适用于/pms/convert/fileurl
	 * @param fileUrl http 或 https 的网络文件路径
	 * @throws IOException 
	 */
	public void setFileUrl(String fileUrl) throws IOException {
		if(fileUrl==null || "".equals(fileUrl.trim())) {
			throw new IOException("设置转换的网络文件为空");
		}
		if(!fileUrl.toLowerCase().startsWith("http://") && !fileUrl.toLowerCase().startsWith("https://")) {
			throw new IOException("设置转换的网络文件格式不正确:"+fileUrl);
		}
		requestBody.put("fileUrl", fileUrl);
	}

	/**
	 * 设置用户自定义数据，该数据会通过接口返回地址范围给用户
	 * @param customData 自定义数据，请不要设置内容过长，会导致转换地址过长，建议仅传递必要的参数
	 * @throws JSONException
	 */
	public void setCustomData(String customData) throws JSONException {
		extraParam.put("fcsCustomData", customData);
	}
	/**
	 * 转换处理完成后，是否立即产出缓存源文件，不设置则按照默认配置执行
	 * @param isDelSrc true 立即删除 false 根据系统配置规则产出
	 * @throws JSONException
	 */
	public void setDelSrc(boolean isDelSrc) throws JSONException{
		if(isDelSrc) {
			extraParam.put("isDelSrc", 1);
		}else {
			extraParam.put("isDelSrc", 0);
		}
	}

	/**
	 * 如果转换的文件有密码，可以传递密码打开
	 * @param password office/pdf打开需要的密码
	 * @throws JSONException
	 */
	public void setDocPassword(String password) throws JSONException{
		if(password!=null) {
			extraParam.put("password", password);
		}
	}

	/**
	 * 去除文档中已经存在的水印
	 * @param remove true去除，false不去除
	 * @throws JSONException
	 */
	public void removeWatermark(boolean remove) throws JSONException {
			extraParam.put("removeWatermark", remove);
	}

	/**
	 * 设置水印
	 * <br><b>注意：设置完成水印后，再修改WaterMark对象无效</b>
	 * @param wm 水印对象
	 * @throws JSONException 
	 */
	public void setWaterMark(WaterMark wm) throws JSONException {
		wm.addExtraParam(extraParam);
	}
	/**
	 * 设置书签
	 * @param BookMark 书签操作对象
	 * @throws JSONException 
	 */
	public void setBookMark(BookMark bookMark) throws JSONException {
		this.bookMark=bookMark;
	}
	private void combinationExtraParam() throws JSONException  {
		if(bookMark!=null) {
			bookMark.addExtraParam(extraParam);
		}
		requestBody.put("extraParam", extraParam.toString());
	}
	/**
	 * 得到要提交的带文件流的参数
	 * @return Map对象
	 * @throws JSONException 
	 */
	public Map<String, Object> getRequestBody() throws JSONException{
		combinationExtraParam();
		return requestBody;
	}
	/**
	 * 得到非文件流的参数数据
	 * @return 字符串
	 * @throws JSONException 
	 */
	public String  getRequestBodyString() throws JSONException {
		combinationExtraParam();
		JSONObject json = new JSONObject(requestBody);
		return json.toString();
	}
}
