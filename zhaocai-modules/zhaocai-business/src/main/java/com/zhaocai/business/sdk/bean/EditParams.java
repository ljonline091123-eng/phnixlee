package com.zhaocai.business.sdk.bean;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 编辑的参数对象
 * @author lizb
 *
 */
public class EditParams {
	/**
	 * 编辑的URL,请定义常量，便于以后根绝业务需要修改统一的接口地址
	 */
	public static final String URL_EDIT = "/pms/wo3/upload";
	/**
	 * 文件编辑类型，需要结合<b>URL_EDIT</b>使用
	 */
	public static final String CONVERT_TYPE_EDIT = "edit_document";
	private Map<String, Object> requestBody;
	//扩展参数
	private JSONObject extraParam;
	//水印
	private JSONObject waterMark;
	//权限控制
	private JSONObject userMenuPermission;
	public EditParams() {
		 requestBody = new HashMap<>();
		 extraParam=new JSONObject();
		 waterMark=new JSONObject();
		 userMenuPermission=new JSONObject();
	}
	/**
	 * 编辑的文件，仅适用于/pms/wo3/upload
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
	 * 设置编辑文件的路径，仅适用于/pms/wo3/fileurl
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
	 * 设置文件名称
	 * @param fileName 文件名称，例如：测试文档.docx
	 */
	public void setFileName(String fileName)  {
		requestBody.put("filename", fileName);
	}
	/**
	 * 设置业务系统的文件ID
	 * @param customerFileId 业务系统的文件id，默认为随机字符串
	 */
	public void setCustomerFileId(String customerFileId)  {
		requestBody.put("customerFileId", customerFileId);
	}
	/**
	 * 设置编辑用户信息
	 * @param userId 用户唯一ID
	 * @param userName 用户名称，显示的用户名，文档修订批注都会以此名称为准
	 * @param facePath 头像地址，建议不传递
	 */
	public void setUserInfo(String userId,String userName,String facePath)  {
		if(userId!=null && "".equals(userId.trim())) {
			requestBody.put("userId", userId);
		}
		if(userName!=null && "".equals(userName.trim())) {
			requestBody.put("userName", userName);
		}
		if(facePath!=null && "".equals(facePath.trim())) {
			requestBody.put("userAvatar", facePath);
		}
	}
	/**
	 * 设置编辑页面左上角返回按钮的地址
	 * @param fallbackUrl 返回按钮的地址
	 */
	public void setFallbackUrl(String fallbackUrl)  {
		if(fallbackUrl!=null && "".equals(fallbackUrl.trim())) {
			requestBody.put("fallbackUrl", fallbackUrl);
		}
	}
	/**
	 * 设置用户编辑权限：编辑
	 */
	public static final int USERRIGHT_EDIT=0;
	/**
	 * 设置用户编辑权限：只读
	 */
	public static final int USERRIGHT_READONLY=1;
	/**
	 * 设置用户编辑权限：进入是只读的，可以切换到编辑
	 */
	public static final int USERRIGHT_TEMPREADONLY=2;
	/**
	 * 设置用户编辑的权限（编辑、只读、临时只读）
	 * @param userRight 0编辑，1只读。2临时只读，在EditParams.USERRIGHT_中选择
	 */
	public void setUserRight(int userRight)  {
		requestBody.put("userRight", userRight);
	}
	/**
	 * 设置是否自动保存，不设置是自动保存
	 * @param saveFlag true 自动保存 false不自动保存
	 */
	public void setSaveFlag(boolean saveFlag)  {
		requestBody.put("saveFlag", saveFlag);
	}
	/**
	 * 设置打开文档所需要的密码，如果文档未设置密码，则设置无效
	 * @param password 密码
	 */
	public void setPassword(String password)  {
		if(password!=null && "".equals(password.trim())) {
			requestBody.put("password", password);
		}
	}
	/**
	 * 保存后回调地址
	 * @param callbackUrl
	 */
	public void setCallbackUrl(String callbackUrl)  {
		if(callbackUrl!=null && "".equals(callbackUrl.trim())) {
			requestBody.put("callbackUrl", callbackUrl);
		}
	}
	/**
	 * 如果要保证每次打开的文件不会受到其他人未关闭影响，需要传递这个参数，可以是uuid，设置后不会协同编辑
	 * <br>如果需要协同编辑，请不要设置此参数
	 * <br>协同编辑时只有所有人都关闭的文档，才会保存
	 * @param fileUUID 文件的随机ID
	 */
	public void setFileUUID(String fileUUID)  {
		if(fileUUID!=null && "".equals(fileUUID.trim())) {
			requestBody.put("fileUUID", fileUUID);
		}
	}
	
	/**
	 * 设置编辑用户信息
	 * @param userId 用户唯一ID
	 * @param userName 用户名称，显示的用户名，文档修订批注都会以此名称为准
	 */
	public void setUserInfo(String userId,String userName)  {
		setUserInfo(userId, userName, null);
	}
	/**
	 * 设置用户自定义数据，该数据会通过接口返回地址范围给用户
	 * @param customData 自定义数据，请不要设置内容过长，会导致转换地址过长，建议仅传递必要的参数
	 */
	public void setCustomData(String customData)  {
		requestBody.put("fcsCustomData", customData);
	}
	/**
	 * 设置要隐藏的菜单
	 * @param menuIds
	 * @throws JSONException 
	 */
	public void setMenuHidden(String... menuIds) throws JSONException {
		for (String menuId : menuIds) {
			userMenuPermission.put(menuId, 0);
		}
	}
	/**
	 * 设置要禁用的菜单（菜单显示，但是不可用）
	 * @param menuIds
	 * @throws JSONException 
	 */
	public void setMenuDisable(String... menuIds) throws JSONException {
		for (String menuId : menuIds) {
			userMenuPermission.put(menuId, 2);
		}
	}
	/**
	 * 设置可用的菜单，一般不用设置，除非默认不显示的菜单
	 * @param menuIds
	 * @throws JSONException 
	 */
	public void setMenuEnable(String... menuIds) throws JSONException {
		for (String menuId : menuIds) {
			userMenuPermission.put(menuId, 1);
		}
	}
	/**
	 *  设置打印按钮，默认显示
	 * @param show 是否显示打印按钮
	 * @param showWaterMark 是否带水印（暂未实现）
	 * @throws JSONException
	 */
	public void setPrintMenu(boolean show,boolean showWaterMark) throws JSONException {
		if(show) {
			setMenuEnable("File_Printer");
		}else {
			setMenuHidden("File_Printer");
		}
	}
	/**
	 * 设置是否显示下载按钮（菜单），不设置默认不显示
	 * @param show true显示，false不显示
	 * @param downloadFileName 如果显示下载，可以设置下载文件名称，null为默认文件名（未实现）
	 * @throws JSONException
	 */
	public void setDownloadMenu(boolean show,String downloadFileName) throws JSONException {
		if(show) {
			setMenuEnable("File_Dl");
		}else {
			setMenuHidden("File_Dl");
		}
	}
	/**
	 * @throws JSONException 
	 * 设置编辑页面打开后的缩放比例，如果不设置，会自动根据文档中记录的比例显示
	 * @param zoom 100为100%，如果要按照110%显示，则设置110
	 * @throws  
	 */
	public void setPageZoom(int zoom) throws JSONException  {
		if(zoom>=0) {
			extraParam.put("Percentage", zoom);
		}
	}
	/**
	 * 设置文档保护
	 * @param open true开启文档保护，false关闭文档保护
	 * @param password open为true时的保护密码，不设置默认 yozosoft
	 * @throws JSONException
	 */
	public void setDocProtec(boolean open,String password) throws JSONException  {
		if(open) {
			extraParam.put("docProtec", 1);
			if(password==null) {
				password="yozosoft";
			}
			extraParam.put("password", password);
		}else {
			extraParam.put("docProtec", 0);
		}
	}
	/**
	 * 打开修订
	 * @throws JSONException
	 */
	public void trackRevisionsOpen() throws JSONException  {
		extraParam.put("TrackRevisions", 1);
	}
	/**
	 * 关闭修订
	 * @throws JSONException
	 */
	public void trackRevisionsClose() throws JSONException  {
		extraParam.put("TrackRevisions", 0);
	}
	/**
	 * 显示修订记录
	 * @throws JSONException
	 */
	public void trackRevisionsShow() throws JSONException  {
		extraParam.put("Mark", 1);
	}
	/**
	 * 隐藏修订记录
	 * @throws JSONException
	 */
	public void trackRevisionsHidden() throws JSONException  {
		extraParam.put("Mark", 0);
	}
	/**
	 * 清稿，所有修订记录都接受
	 * @throws JSONException
	 */
	public void trackRevisionsClear() throws JSONException  {
		extraParam.put("CleanG", 1);
	}
	/**
	 * 接受和拒绝修订记录的按钮不显示
	 * @throws JSONException
	 */
	public void trackRevisionsAcceptRejectHidden() throws JSONException  {
		extraParam.put("hideRev", 1);
	}
	/**
	 * 设置可设置的书签名称列表
	 * @param bookMarkListRange 书签名称，多个名称之间用英文逗号分隔，例如：甲方,乙方,金额,签订日期
	 * @throws JSONException
	 */
	public void setBookMarkListRange(String bookMarkListRange) throws JSONException  {
		if(bookMarkListRange!=null && !"".equals(bookMarkListRange.trim())) {
			extraParam.put("bookMarkListRange", bookMarkListRange);
		}
	}
	/**
	 * 接受和拒绝修订记录的按钮有效
	 * @throws JSONException
	 */
	public void trackRevisionsAcceptRejectEnable() throws JSONException  {
	//不需要设置即显示按钮
	}
	/**
	 * 清除文档中已有的水印
	 * @throws JSONException
	 */
	public void clearWaterMark() throws JSONException  {
		extraParam.put("clearWaterMark", 1);
	}
	/**
	 * 设置复制粘贴剪切的状态
	 * @param copy true 可以复制，false禁止复制
	 * @param paste true可以粘贴，false禁止粘贴
	 * @param cut true可以剪切，false禁止剪切
	 * @throws JSONException
	 */
	public void setCopyPasteState(boolean copy,boolean paste,boolean cut) throws JSONException  {
		if(!copy) {
			extraParam.put("copyAllow", 0);
		}
		if(!paste) {
			extraParam.put("pasteAllow", 0);
		}
		if(!cut) {
			extraParam.put("cutAllow", 0);
		}
	}
	/**
	 * 文档内水印，跟随文档保存
	 */
	public static final int WATER_MARK_TYPE_DOCUMENT=2;
	/**
	 * 页面水印，仅显示在页面上
	 */
	public static final int WATER_MARK_TYPE_PAGE=1;
	/**
	 * 设置水印
	 * <br><b>注意：设置完成水印后，再修改WaterMark对象无效</b>
	 * @param type 1文档水印 2页面水印，在EditParams.WATER_MARK_TYPE_中选择
	 * @param wm 水印对象
	 * @throws JSONException 
	 */
	public void setWaterMark(int type,WaterMark wm) throws JSONException {
		wm.addEditExtraParam(waterMark);
		switch (type) {
		case WATER_MARK_TYPE_DOCUMENT:
			extraParam.put("waterMarkType", WATER_MARK_TYPE_DOCUMENT);
			break;
		case WATER_MARK_TYPE_PAGE:
			extraParam.put("waterMarkType", WATER_MARK_TYPE_PAGE);
			break;
		default:
			break;
		}
	}
	
	private void combinationExtraParam() throws JSONException {
		//如果水印有设置，则追加至参数中
		if(waterMark.length()>0) {
			extraParam.put("waterMark", waterMark);
		}
		requestBody.put("extraParam", extraParam.toString());
		//如果有权限设置，则追加至参数中
		if(userMenuPermission.length()>0) {
			requestBody.put("waterMark", userMenuPermission.toString());
		}
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
