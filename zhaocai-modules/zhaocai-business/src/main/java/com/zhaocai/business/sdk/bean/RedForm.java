package com.zhaocai.business.sdk.bean;

import com.zhaocai.business.pub.utils.Sender;
import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class RedForm {
	/**
	 * 正文位置书签的名称
	 */
	private static final String CONTENT_BOOK_NAME="content";
	/**
	 * 书签列表
	 */
	private BookMark  bookMark;
	/**
	 * 正文路径
	 */
    private String contentPath;
    /**
     * 模版路径
     */
    private String templatePath;
    
    /**
     * 得到模版文件路径（服务器路径）
	 * @return templatePath
	 */
	public String getTemplatePath() {
		return templatePath;
	}
	/**
	 * 设置模版文件路径（服务器路径）
	 * @param templatePath 要设置的模版文件路径（服务器路径）
	 */
	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}
	/**
     * 正文的书签名称，默认为：content
     */
    private String contentBookName;
	public RedForm() {
		bookMark=new BookMark();
	}
    /**
	 * 设置套红文件的书签
	 * @param bookMark 要设置的 bookMasks
	 */
	public void setBookMasks(BookMark bookMark) {
		this.bookMark = bookMark;
	}
	/**
	 * 追加书签对应内容
	 * @param name
	 * @param text
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void addBookMark(String name,String text) throws NoSuchAlgorithmException, JSONException, IOException {
		bookMark.addBookMarkText(name, text);
	}
	/**
	 * 得到正文的书签名称
	 * @return contentBookName
	 */
	public String getContentBookName() {
		if(contentBookName==null || "".equals(contentBookName.trim())) {
			contentBookName=CONTENT_BOOK_NAME;
		}
		return contentBookName;
	}
	/**
	 * 设置正文的文件地址（服务器上文件）
	 * @param contentPath 要设置的文件地址（服务器上文件）
	 */
	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	/**
	 * 得到正文文件的地址
	 * @return contentPath
	 */
	public String getContentPath() {
		return contentPath;
	}
	/**
	 * 设置正文书签名称
	 * @param contentBookName 要设置的正文书签名称
	 */
	public void setContentBookName(String contentBookName) {
		this.contentBookName = contentBookName;
	}
	/**
	 * 请求得到套红后的文件地址
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException 
	 */
    public String post() throws IOException, JSONException, NoSuchAlgorithmException {
		ConvertParams params = new ConvertParams();
		// 设置要处理的文档模版
		params.setFilePath(getTemplatePath());
		bookMark.addBookMarkDocument(getContentBookName(), getContentPath());
		// 组织书签
		params.setBookMark(bookMark);
		// 提交处理文档
		String response = Sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
		return response;
    }
}
