/**
 * 文档平台调用示例
 */
package com.zhaocai.business.demo.app;

import com.zhaocai.business.sdk.bean.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;


/**
 * @author lizb 该文件仅为示例，请不要直接使用，请根据业务参考此示例对接 示例代码没有完全考虑业务数据的格式，请实际对接时对数据参数进行校验
 */
public class DpDemo {

	/**
	 * 主方法
	 * 
	 * @param args
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, JSONException {
		//示例文件
		String filePath=DemoTestFile.getFilePath("抢抓机遇期培育新动能.docx");
		//编辑角色
//		editDocument(filePath,EditParams.ROLE_EDITER);
		//批注角色
		editDocument(filePath, EditParams.ROLE_ANNOTATION);
		//审核角色
//		editDocument(filePath,EditParams.ROLE_AUDIT);
		//管理角色，可以设置限制编辑
//		editDocument(filePath,EditParams.ROLE_SUPER);
		//只读角色
//		editDocument(filePath,EditParams.ROLE_VIEW);
//		 previewOffice();
//		 System.out.println("*********预览Office文件******************************************");
		// System.out.println("*********预览Office文件图片格式******************************************");
//		 previewOfficePic();
//		 System.out.println("*********预览PDF******************************************");
//		 previewPdf();
		// System.out.println("*********预览OFD******************************************");
		// previewOfd();
		// System.out.println("*********预览图片******************************************");
		// previewPic();
//		 System.out.println("*********预览压缩文件******************************************");
//		 previewZip();
//		 System.out.println("*********处理书签******************************************");
//		 bookMarkOperate();
//		System.out.println("*********office转pdf******************************************");
//		convertOfficeToPdf();
//		System.out.println("*********office转ofd******************************************");
//		convertOfficeToOfd();
		// System.out.println("*********office带水印下载******************************************");
		// downloadWaterMarkOffice();
//		 System.out.println("*********PDF带水印下载******************************************");
//		 downloadWaterMarkPdf();
//		 System.out.println("*********OFD带水印下载******************************************");
//		 downloadWaterMarkOfd();
//		 System.out.println("*********编辑文件******************************************");
//		 editDocument();
//		 System.out.println("*********文件套红******************************************");
//		 redSet();
	}

	/**
	 * 预览Office文件（word、excel、ppt）
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static void previewOffice() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.docx"));
		params.setFileName("抢抓机遇期培育新动能.docx");
		params.setHtmlName("抢抓机遇期培育新动能");
		params.setHtmlTitle("抢抓机遇期培育新动能");
		// 允许复制
		params.setCopy(false);
		// 签批
		params.setSignature(true, "http://www.abc.com/xxx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 是否显示修订
		params.setAcceptTracks(false);
		// 设置可下载
		params.setDownloadMenu(true, "测试1.docx");
		// 只允许打开一次
//		params.setPreviewNumber(1);
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO水印");
		//服务器图片水印
//		WaterMark wm = new WaterMark(WaterMark.TYPE_PIC, DemoTestFile.getFilePath("wm.png"));
		//网络地址图片水印
		// WaterMark wm = new WaterMark(WaterMark.TYPE_PIC,
		// "https://www.gov.cn/images/gtrs_logo_lt.png");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
		System.out.println("预览Office文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
//		String dwz=DWZDemo.setDwz(viewUrl);
//		System.out.println(dwz);
	}

	/**
	 * 以图片方式预览Office（移动端采用此方式预览效果较好）
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static void previewOfficePic() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.docx"));
		params.setFileName("抢抓机遇期培育新动能.docx");
		params.setHtmlName("[html头部显示]抢抓机遇期培育新动能");
		params.setHtmlTitle("页面标签显示内容");
		// 签批
		params.setSignature(true, "http://www.abc.com/xxx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 是否显示修订
		params.setAcceptTracks(false);
		// 设置可下载
		params.setDownloadMenu(true, "测试1.docx");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-PIC水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE_PIC, params.getRequestBody());
		System.out.println("图片方式预览Office文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 预览pdf文件
	 */
	public static void previewPdf() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.pdf"));
		params.setFileName("抢抓机遇期培育新动能.pdf");
		params.setHtmlName("[html头部显示pdf]抢抓机遇期培育新动能");
		params.setHtmlTitle("pdf抢抓机遇期培育新动能");
		// 签批
		params.setSignature(true, "http://www.abc.com/xxx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 允许复制
		params.setCopy(true);
		// 设置可下载
		params.setDownloadMenu(true, "测试1.pdf");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-PDF水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
		System.out.println("预览pdf文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 预览ofd文件
	 */
	public static void previewOfd() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.ofd"));
		params.setFileName("抢抓机遇期培育新动能.pdf");
		params.setHtmlName("[html头部显示ofd]抢抓机遇期培育新动能");
		params.setHtmlTitle("抢抓机遇期培育新动能");
		// 签批
		params.setSignature(true, "http://www.abc.com/xxx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 允许复制
		params.setCopy(true);
		// 设置可下载
		params.setDownloadMenu(true, "测试1.ofd");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-OFD水印");
		
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFD, params.getRequestBody());
		System.out.println("预览ofd文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 预览图片文件
	 */
	public static void previewPic() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(DemoTestFile.getFilePath("图片示例.jpg"));
		params.setFileName("图片示例.jpg");
		params.setHtmlName("图片示例");
		params.setHtmlTitle("图片示例");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 设置可下载
		params.setDownloadMenu(true, "图片.jpg");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
		System.out.println("预览图片文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 预览压缩文件
	 */
	public static void previewZip() throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		 params.setFilePath(DemoTestFile.getFilePath("files.zip"));
		// params.setFilePath(DemoTestFile.getFilePath("files.rar"));
//		params.setFilePath(DemoTestFile.getFilePath("files.7z"));
		params.setFileName("压缩文件");
		params.setHtmlName("压缩文件示例");
		params.setHtmlTitle("压缩文件示例");
		// 是否可打印
		params.setPrintMenu(true, true);
		// 设置可下载
		params.setDownloadMenu(true, "压缩文件");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_ZIP, params.getRequestBody());
		System.out.println("预览压缩文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 编辑office文件
	 * 
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static void editDocument() throws JSONException, IOException, NoSuchAlgorithmException {
		// 组织请求参数
		EditParams params = new EditParams();
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.docx"));
		params.setFileName("抢抓机遇期培育新动能.docx");
		params.setUserInfo("userid1", "用户1");
		params.setFallbackUrl("http://www.yozosoft.com");
		params.setUserRight(EditParams.USERRIGHT_EDIT);
		// 自动保存
		params.setSaveFlag(true);
		// 回调地址支持2中方式获取文件，请根据需要按照接口规范实现接口
		params.setCallbackUrl("http://xxx.xxx.xx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 设置可下载
		params.setDownloadMenu(true, "");
		// 设置文档显示比例，不设置则按照文档中保存的比例显示
		params.setPageZoom(100);
		// 开档打开修订
//		params.trackRevisionsOpen();
		// 开档关闭修订
		 params.trackRevisionsClose();
		// 显示修订记录
		params.trackRevisionsShow();
		// 隐藏修订记录
		// params.trackRevisionsHidden();
		params.trackRevisionsAcceptRejectEnable();
		// 清稿（修订记录全部接受）
		// params.trackRevisionsClear();
		// 设置复制粘贴剪切是否可用
		params.setCopyPasteState(false, true, false);
		// 设置书签时可选择的内容，暂未实现
		params.setBookMarkListRange("甲方,乙方,金额,签订日期",true,true);
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(EditParams.WATER_MARK_TYPE_PAGE, wm);

		String response = Sender1.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
		System.out.println("编辑响应结果：");
		System.out.println(response);
		String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
		System.out.println(editUrl);
	}

	/**
	 * 文档中插入文本（后台处理）
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static void bookMarkOperate() throws IOException, JSONException, NoSuchAlgorithmException {

		StringBuffer jf = new StringBuffer();
		jf.append("1、甲方应按照约定向乙方支付服务费用。").append("\n");
		jf.append("2、甲方应提供乙方提供服务所需的必要信息和资料。").append("\n");
		jf.append("3、甲方应对乙方提供服务过程中产生的损失和费用承担责任。");
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		// 组织书签
		BookMark bookMark = new BookMark();
		bookMark.addBookMarkText("乙方", "乙方软件股份有限公司");
		bookMark.addBookMarkText("乙方_1", "乙方软件股份有限公司");
		bookMark.addBookMarkText("甲方", "甲方有限公司");
		bookMark.addBookMarkPic("甲方_1", DemoTestFile.getFilePath("seal.png"));
		bookMark.addBookMarkPic("甲方签名", DemoTestFile.getFilePath("sgin1.png"),100, 40);
		bookMark.addBookMarkText("服务内容", "软件开发服务");
		bookMark.addBookMarkText("服务描述", "开发文档管理系统");
		bookMark.addBookMarkText("服务时间", "2024年12月-2025年12月12日");
		bookMark.addBookMarkText("甲方义务", jf.toString());
		bookMark.addBookMarkDocument("乙方义务", DemoTestFile.getFilePath("乙方义务.docx"));
		bookMark.addBookMarkText("质量标准", "国家电子文档管理规范质量要求");
		bookMark.addBookMarkText("费用大写", "捌佰伍拾万元整");
		bookMark.addBookMarkText("费用小写", "8,500,000.00");
		params.setBookMark(bookMark);
		// 提交处理文档
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 套红，同样也可以作为插入文档内容到主文件的实现
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void redSet() throws NoSuchAlgorithmException, JSONException, IOException {
		// 组织表单数据
		RedForm redForm = new RedForm();
		//设置正文
		redForm.setContentPath(DemoTestFile.getFilePath("正文.docx"));
		//设置文件模版
		redForm.setTemplatePath(DemoTestFile.getFilePath("发文模版.docx"));
		//设置书签内容
		redForm.addBookMark("标题", "抓抢机遇期 培育新动能");
		redForm.addBookMark("落款单位", "软件开发中心综合管理部");
		redForm.addBookMark("日期", "2024年11月2日");
		redForm.addBookMark("发文字号", "软件办〔2024〕007号");
		redForm.addBookMark("签发人", "李宗波");
		redForm.addBookMark("发送单位", "各部门");
		redForm.addBookMark("抄送", "软件办公室");
		redForm.addBookMark("主题词", "抓抢机遇");
		redForm.addBookMark("印发时间", "2024年11月11日");
		//请求并获取套红后的数据
		String response = redForm.post();
		System.out.println("套红响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		//将viewUrl下载到业务系统内
		System.out.println(viewUrl);
		String fileName = "套红后文件"+System.currentTimeMillis()+".docx";
		fileName = DemoTestFile.download(viewUrl,fileName);
		//编辑套红后的文件
		EditParams params = new EditParams();
		params.setFilePath(fileName);
		params.setFileName("套红后文件.docx");
		params.setUserInfo("userid1", "用户1");
		params.setFallbackUrl("http://www.yozosoft.com");
		params.setUserRight(EditParams.USERRIGHT_EDIT);
		String res = Sender1.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
		System.out.println("编辑响应结果：");
		System.out.println(res);
		String editUrl = new JSONObject(res).optJSONObject("data").optString("editUrl");
		System.out.println("编辑文件地址：");
		System.out.println(editUrl);
	}

	/**
	 * office文件转pdf
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static void convertOfficeToPdf() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_DOC_PDF, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 去除word修订记录
	 *
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static void convertOfficeToOffice() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		// 设置水印
//		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
//		// 将水印设置到参数中
//		params.setWaterMark(wm);
		params.setAccepTracks(false);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * office文件转Ofd
	 */
	public static void convertOfficeToOfd() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_DOC_OFD, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * office带水印下载
	 */
	public static void downloadWaterMarkOffice() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * pdf带水印下载
	 */
	public static void downloadWaterMarkPdf() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.pdf"));
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_PDF, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * OFD带水印下载
	 */
	public static void downloadWaterMarkOfd() throws IOException, NoSuchAlgorithmException, JSONException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(DemoTestFile.getFilePath("抢抓机遇期培育新动能.ofd"));
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO-水印");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender1.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_OFD, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}
	/**
	 * 编辑文档
	 * @param filePath 文档地址
	 * @param role 文档所属角色
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String editDocument(String filePath,String role) throws JSONException, IOException, NoSuchAlgorithmException {
		if(EditParams.ROLE_VIEW.equals(role)) {
			return previewOffice(filePath);
		}else {
			// 组织请求参数
			EditParams params = new EditParams();
			params.setFilePath(filePath);
			params.setUserInfo("userid1", "用户1");
			//隐藏“限制编辑”按钮
			params.hiddenLimitedit(true);
			//设置角色
			params.setRole(role);
			
			String response = Sender1.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
			System.out.println("编辑响应结果：");
			System.out.println(response);
			String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
			System.out.println(editUrl);
			return editUrl;
		}
	}
	/**
	 * 前端演示的打开文档方法
	 * @param filePath
	 * @param domain
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String webEditDocument(String filePath,String domain) throws JSONException, IOException, NoSuchAlgorithmException {
			// 组织请求参数
		EditParams params = new EditParams();
		params.setFilePath(filePath);
		params.setUserInfo("userid2", "演示");
		//设置角色
		params.showInsPic(domain);
		String response = Sender1.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
		System.out.println("编辑响应结果："+response);
		String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
		return editUrl;
	}
	public static String previewOffice(String filePath) throws IOException, JSONException, NoSuchAlgorithmException {
		// 组织请求参数
		PreviewParams params = new PreviewParams();
		// 设置要预览的文件
		params.setFilePath(filePath);
		// 允许复制
		params.setCopy(false);
		// 签批
		params.setSignature(true, "http://www.abc.com/xxx");
		// 是否可打印
		params.setPrintMenu(true, false);
		// 是否显示修订
		params.setAcceptTracks(false);
		// 设置可下载
		params.setDownloadMenu(true, "测试1.docx");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO水印");
		params.setWaterMark(wm);
		String response = Sender1.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
		System.out.println("编辑响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
		return viewUrl;
	}
}
