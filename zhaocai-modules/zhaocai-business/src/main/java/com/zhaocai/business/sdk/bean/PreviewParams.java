package com.zhaocai.business.sdk.bean;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 预览参数对象
 * @author lizb
 *
 */
public class PreviewParams {
	/**
	 * 预览的URL,请定义常量，便于以后根绝业务需要修改统一的接口地址
	 */
	public static final String URL_PREVIEW = "/pms/preview/upload";
	/**
	 * 基于网络文件预览的URL,请定义常量，便于以后根绝业务需要修改统一的接口地址
	 */
	public static final String URL_PREVIEW_URL = "/pms/preview/fileurl";


	/**
	 * Office文件预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_OFFICE = "preview_office_safe";
	/**
	 * Office文件以图片方式预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_OFFICE_PIC = "preview_office_pic";
	/**
	 * pdf文件预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_PDF = "preview_pdf_hd";
	/**
	 * ofd文件预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_OFD = "preview_ofd";
	/**
	 * 图片文件预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_PIC = "preview_pic";
	/**
	 * 压缩文件（zip,7z,rar,tar,gz）预览，需要结合<b>URL_PREVIEW</b>使用
	 */
	public static final String CONVERT_TYPE_PREVIEW_ZIP = "preview_zip";
	private Map<String, Object> requestBody;
	private JSONObject extraParam;
	public PreviewParams() {
		 requestBody = new HashMap<>();
		 extraParam=new JSONObject();
	}
	/**
	 * 设置预览文件，仅适用于/pms/preview/upload
	 * @param filePath 文件路径
	 * @throws IOException
	 */
	public void setFilePath(String filePath) throws IOException {
		if(filePath==null || "".equals(filePath.trim())) {
			throw new IOException("设置预览的服务器文件地址为空");
		}
		requestBody.put("file", new File(filePath));
	}
	/**
	 * 设置文件路径，仅适用于/pms/preview/upload
	 * @param fileUrl http 或 https 的网络文件路径
	 * @throws IOException
	 */
	public void setFileUrl(String fileUrl) throws IOException {
		if(fileUrl==null || "".equals(fileUrl.trim())) {
			throw new IOException("设置预览的网络文件为空");
		}
		if(!fileUrl.toLowerCase().startsWith("http://") && !fileUrl.toLowerCase().startsWith("https://")) {
			throw new IOException("设置预览的网络文件格式不正确:"+fileUrl);
		}
		requestBody.put("fileUrl", fileUrl);
	}
	/**
	 * 设置文件名称
	 * @param fileName 文件名
	 */
	public void setFileName(String fileName) {
		requestBody.put("filename", fileName);
	}
	/**
	 * 设置用户自定义数据，该数据会通过接口返回地址范围给用户
	 * @param customData 自定义数据，请不要设置内容过长，会导致预览地址过长，建议仅传递必要的参数
	 * @throws JSONException
	 */
	public void setCustomData(String customData) throws JSONException {
		extraParam.put("fcsCustomData", customData);
	}
	/**
	 * 预览处理完成后，是否立即产出缓存源文件，不设置则按照默认配置执行
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
	 * 设置预览页面显示的文件名称，不设置则为原文件名
	 * @param htmlName 页面显示名称
	 * @throws JSONException
	 */
	public void setHtmlName(String htmlName) throws JSONException{
		if(htmlName!=null) {
			extraParam.put("htmlName", htmlName);
		}
	}
	/**
	 * 设置预览页签显示的名称
	 * @param htmlName 页签显示的名称
	 * @throws JSONException
	 */
	public void setHtmlTitle(String htmlTitle) throws JSONException{
		if(htmlTitle!=null) {
			extraParam.put("htmlTitle", htmlTitle);
		}
	}
	/**
	 * 如果预览的文件有密码，可以传递密码打开，如果不传递，则预览时弹出密码输入页面，用户自己输入
	 * @param password office/pdf打开需要的密码
	 * @throws JSONException
	 */
	public void setDocPassword(String password) throws JSONException{
		if(password!=null) {
			extraParam.put("password", password);
		}
	}
	/**
	 * 设置预览时是否显示导航窗格/缩略图
	 * @param show true显示，false不显示
	 * @throws JSONException
	 */
	public void setShowList(boolean show) throws JSONException {
		if(show) {
			extraParam.put("isShowList", 1);
		}else {
			extraParam.put("isShowList", 0);
		}
	}
	/**
	 * 设置是否显示修订记录
	 * @param show true显示，false不显示
	 * @throws JSONException
	 */
	public void setAcceptTracks(boolean show) throws JSONException {
		if(show) {
			extraParam.put("acceptTracks", 1);
		}else {
			extraParam.put("acceptTracks", 0);
		}
	}

	public void setNocache(boolean flag) throws JSONException {
		if(flag) {
			extraParam.put("noCache", 1);
		}else {
			extraParam.put("noCache", 0);
		}
	}

	/**
	 * 设置压缩文件预览是否开启高清
	 * @param hd true高清，false标清
	 * @throws JSONException
	 */
	public void setzipHDPreview(boolean hd) throws JSONException {
		if(hd) {
			extraParam.put("zipConvertType", 1);
		}else {
			extraParam.put("zipConvertType", 0);
		}
	}
	/**
	 * 设置是否显示下载按钮（菜单），不设置默认不显示
	 * @param show true显示，false不显示
	 * @param downloadFileName 如果显示下载，可以设置下载文件名称，null为默认文件名
	 * @throws JSONException
	 */
	public void setDownloadMenu(boolean show,String downloadFileName) throws JSONException {
		if(show) {
			extraParam.put("isDownload", 1);
			if(downloadFileName!=null && !"".equals(downloadFileName.trim())) {
				//允许复制
				extraParam.put("downloadFileName", downloadFileName);
			}
		}else {
			extraParam.put("isDownload", 0);
		}
	}
	/**
	 * 设置是否显示打印按钮，不设置默认不显示
	 * @param show true显示，false不显示
	 * @param showWaterMark 打印是，是否显示水印，true显示，false不显示，图片打印不显示水印
	 * @throws JSONException
	 */
	public void setPrintMenu(boolean show,boolean showWaterMark) throws JSONException {
		if(show) {
			extraParam.put("isPrint", 1);
			if(showWaterMark) {
				extraParam.put("isShowPrintMark", 1);
			}else {
				extraParam.put("isShowPrintMark", 0);
			}
		}else {
			extraParam.put("isPrint", 0);
			//打印按钮不显示，有时候需要定制实现为从系统外边控制打印，所以不显示打印按钮也设置水印状态
			if(showWaterMark) {
				extraParam.put("isShowPrintMark", 1);
			}else {
				extraParam.put("isShowPrintMark", 0);
			}
		}
	}
	/**
	 * 开启excel预览公式数据重新计算，不设置默认不开启
	 * <br>一般情况不需要开启
	 * @param reCalc true开启 false不开启
	 * @throws JSONException
	 */
	public void setExcelRecalc(boolean reCalc) throws JSONException {
		if(reCalc) {
			extraParam.put("isOpenCalc", 1);
		}else {
			extraParam.put("isOpenCalc", 0);
		}
	}
	/**
	 * 设置excel预览是否默认显示最后打开的sheet，默认显示第一个sheet
	 * @param ActiveView true最后打开的sheet，false 第一个sheet
	 * @throws JSONException
	 */
	public void setExcelPreviewActiveView(boolean ActiveView) throws JSONException {
		if(ActiveView) {
			extraParam.put("isActiveView", 1);
		}else {
			extraParam.put("isActiveView", 0);
		}
	}
	/**
	 * 设置预览页面是否显示签批功能
	 * @param show true显示，false不显示
	 * @param callBackUrl 如果需要签批，需要设置签批回调的URL，http或https
	 * @throws JSONException
	 * @throws IOException
	 */
	public void setSignature(boolean show,String callBackUrl) throws JSONException, IOException {
		if(show) {
			extraParam.put("isSignature", 1);
			if(callBackUrl==null) {
				throw new IOException("启用签批请设置签批回调地址");
			}
			if(!callBackUrl.toLowerCase().startsWith("http://") && !callBackUrl.toLowerCase().startsWith("https://")) {
				throw new IOException("设置预览的网络文件格式不正确:"+callBackUrl);
			}
			extraParam.put("signCallback", callBackUrl);
		}else {
			extraParam.put("isSignature", 0);
		}
	}
	/**
	 * 是否显示预览工具栏，不设置默认显示
	 * @param show true显示，false不显示
	 * @throws JSONException
	 */
	public void setHeaderBar(boolean show) throws JSONException {
		if(show) {
			extraParam.put("isHeaderBar", 1);
		}else {
			extraParam.put("isHeaderBar", 0);
		}
	}

	/**
	 * 是否显示PPT的备注，不设置默认不显示
	 * @param show true显示，false不显示
	 * @throws JSONException
	 */
	public void setShowPPTComment(boolean show) throws JSONException {
		if(show) {
			extraParam.put("isShowComment", 1);
		}else {
			extraParam.put("isShowComment", 0);
		}
	}
	/**
	 * 是否显示PPT的隐藏页，不设置默认不显示
	 * @param show true显示，false不显示
	 * @throws JSONException
	 */
	public void setShowPPTHideSlide(boolean show) throws JSONException {
		if(show) {
			extraParam.put("isShowHideSlide", 1);
		}else {
			extraParam.put("isShowHideSlide", 0);
		}
	}
	/**
	 * 设置是否可复制
	 * @param IsCopy true允许复制 false禁止复制
	 * @throws JSONException
	 */
	public void setCopy(boolean IsCopy) throws JSONException{
		if(IsCopy) {
			//允许复制
			extraParam.put("IsCopy", 0);
		}else {
			//不允许复制
			extraParam.put("IsCopy", 1);
		}
	}
	/**
	 * 设置同一个预览地址可以查看的次数，不设置默认不限制预览
	 * <br>商密如果要控制预览权限，可以设置num为1
	 * @param num 可预览次数，0为不限制
	 * @throws JSONException
	 */
	public void setPreviewNumber(long num) throws JSONException {
		if(num>=0) {
			//允许复制
			extraParam.put("num", num);
		}
	}
	/**
	 * 设置预览地址过期时间，不设置默认为系统统一的过期时间
	 * @param time 过期时间单位秒，0为不限制
	 * @throws JSONException
	 */
	public void setExpirationTime(long time) throws JSONException {
		if(time>=0) {
			//允许复制
			extraParam.put("num", time);
		}
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
	private void combinationExtraParam()  {
		requestBody.put("extraParam", extraParam.toString());
	}
	/**
	 * 得到要提交的带文件流的参数
	 * @return Map对象
	 */
	public Map<String, Object> getRequestBody(){
		combinationExtraParam();
		return requestBody;
	}
	/**
	 * 得到非文件流的参数数据
	 * @return 字符串
	 */
	public String  getRequestBodyString() {
		combinationExtraParam();
		JSONObject json = new JSONObject(requestBody);
		return json.toString();
	}
}
