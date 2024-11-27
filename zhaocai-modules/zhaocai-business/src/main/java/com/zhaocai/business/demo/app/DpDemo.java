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
//		 System.out.println("*********预览Office文件******************************************");
//		 previewOffice();
		// System.out.println("*********预览Office文件图片格式******************************************");
		// previewOfficePic();
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
		 System.out.println("*********编辑文件******************************************");
		 editDocument();
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
		params.setHtmlName("[html头部显示]抢抓机遇期培育新动能");
		params.setHtmlTitle("页面标签显示内容");
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
		params.setPreviewNumber(1);
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "DEMO水印");
		// WaterMark wm = new WaterMark(WaterMark.TYPE_PIC,
		// "https://www.gov.cn/images/gtrs_logo_lt.png");
		// 将水印设置到参数中
		params.setWaterMark(wm);
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
		System.out.println("预览Office文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
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
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE_PIC, params.getRequestBody());
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
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
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
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFD, params.getRequestBody());
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
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
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
		String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_ZIP, params.getRequestBody());
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
		params.setFilePath(DemoTestFile.getFilePath("服务合同.docx"));
		params.setFileName("测试.docx");
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
	//	params.setBookMarkListRange("甲方,乙方,金额,签订日期");
		// 设置水印
		WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, "解文静-水印");
		// 将水印设置到参数中
		params.setWaterMark(EditParams.WATER_MARK_TYPE_PAGE, wm);

		String response = Sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
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
		bookMark.addBookMarkInfo("乙方", "乙方软件股份有限公司", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("乙方_1", "乙方软件股份有限公司", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("甲方", "甲方有限公司", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("甲方_1", DemoTestFile.getFilePath("seal.png"), BookMark.BOOKMARK_TYPE_PIC, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("甲方签名", DemoTestFile.getFilePath("sgin1.png"), BookMark.BOOKMARK_TYPE_PIC, BookMark.BOOKMARK_OTYPE_REPLACE, 100, 40);
		bookMark.addBookMarkInfo("服务内容", "软件开发服务", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("服务描述", "开发文档管理系统", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("服务时间", "2024年12月-2025年12月12日", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("甲方义务", jf.toString(), BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("乙方义务", DemoTestFile.getFilePath("乙方义务.docx"), BookMark.BOOKMARK_TYPE_DOCUMENT, BookMark.BOOKMARK_OTYPE_ADD);
		bookMark.addBookMarkInfo("质量标准", "国家电子文档管理规范质量要求", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("费用大写", "捌佰伍拾万元整", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		bookMark.addBookMarkInfo("费用小写", "8,500,000.00", BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
		params.setWaterMark(bookMark);
		// 提交处理文档
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}

	/**
	 * 套红，同样也可以作为插入文档内容到主文件的实现
	 */
	public static void redSet() {
		// 组织表单数据
		// 模版文件
		// 正文内容
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
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_DOC_PDF, params.getRequestBody());
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
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_DOC_OFD, params.getRequestBody());
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
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
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
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_PDF, params.getRequestBody());
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
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_OFD, params.getRequestBody());
		System.out.println("转换文件响应结果：");
		System.out.println(response);
		String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
		System.out.println(viewUrl);
	}


}
