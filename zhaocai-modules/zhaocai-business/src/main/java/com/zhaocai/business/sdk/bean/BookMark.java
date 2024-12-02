package com.zhaocai.business.sdk.bean;

import com.zhaocai.business.demo.app.Sender;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;



public class BookMark {
	/**
	 * 书签类型：文本类型书签
	 */
	public static final int BOOKMARK_TYPE_TXT=1;
	/**
	 * 书签类型：图片类型书签
	 */
	public static final int BOOKMARK_TYPE_PIC=2;
	/**
	 * 书签类型：文档类型
	 */
	public static final int BOOKMARK_TYPE_DOCUMENT=3;
	/**
	 * 书签操作方式：替换内容
	 */
	public static final int BOOKMARK_OTYPE_REPLACE=0;
	/**
	 * 书签操作方式：追加内容
	 */
	public static final int BOOKMARK_OTYPE_ADD=1;
	/**
	 * 书签操作方式：删除书签替换
	 */
	public static final int BOOKMARK_OTYPE_REMOVE=2;
	/**
	 * 书签中表示文件的前缀
	 */
	private static final String MARK_FILE_PREFIX="yfile:";
	//书签数据
	private JSONArray bookMarkJson = new JSONArray();
	//书签索引，为了不破坏放入顺序，使用索引定位
	private Map<String, Integer> index = new HashMap<String, Integer>();
	/**
	 * 添加要插入书签的内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param contentOrPath 书签内容 或者是 图片文档的文件服务器路径
	 * @param type 插入书签内容的类型，传递内容为BOOKMARK_TYPE_*
	 * @param operateType 操作方式，传递内容为BOOKMARK_OTYPE_*
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkInfo(String name,String contentOrPath,int type,int operateType) throws JSONException, NoSuchAlgorithmException, IOException {
		addBookMarkInfo(name, contentOrPath, type, operateType, 0, 0);
	}
	/**
	 * 添加要插入文本书签的内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param text 书签文本内容
	 * @param operateType 操作方式，传递内容为BOOKMARK_OTYPE_*
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkText(String name,String text,int operateType) throws NoSuchAlgorithmException, JSONException, IOException {
		addBookMarkInfo(name, text, BOOKMARK_TYPE_TXT, operateType);
	}
	/**
	 * 添加要插入文本书签的内容（书签操作方式是内容替换）
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param text 书签文本内容
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkText(String name,String text) throws NoSuchAlgorithmException, JSONException, IOException {
		addBookMarkText(name, text, BOOKMARK_OTYPE_REPLACE);
	}
	/**
	 * 添加要插入书签的图片内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param picTath 图片文件的路径（服务器上绝对路径）
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void addBookMarkPic(String name,String picTath) throws NoSuchAlgorithmException, JSONException, IOException {
		addBookMarkPic(name, picTath, 0,0);
	}
	/**
	 * 添加要插入书签的图片内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param picTath 图片文件的路径（服务器上绝对路径）
	 * @param picWidth 图片类型的图片宽度，单位px
	 * @param picHeight 图片类型的图片高度，单位px
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void addBookMarkPic(String name,String picTath,int picWidth,int picHeight) throws NoSuchAlgorithmException, JSONException, IOException {
		addBookMarkPic(name, picTath, BOOKMARK_OTYPE_REPLACE,picWidth,picHeight);
	}
	/**
	 * 添加要插入书签的图片内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param picTath 图片文件的路径（服务器上绝对路径）
	 * @param operateType 操作方式，传递内容为BOOKMARK_OTYPE_*
	 * @param picWidth 图片类型的图片宽度，单位px
	 * @param picHeight 图片类型的图片高度，单位px
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void addBookMarkPic(String name,String picTath,int operateType,int picWidth,int picHeight) throws NoSuchAlgorithmException, JSONException, IOException {
		addBookMarkInfo(name, picTath, BOOKMARK_TYPE_PIC, operateType,picWidth,picHeight);
	}
	/**
	 * 添加要插入的文档
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param docPath 文档的路径（服务器上绝对路径）
	 * @param operateType 操作方式，传递内容为BOOKMARK_OTYPE_*
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkDocument(String name,String docPath,int operateType) throws JSONException, NoSuchAlgorithmException, IOException {
		addBookMarkInfo(name, docPath, BOOKMARK_TYPE_DOCUMENT, operateType);
	}
	/**
	 * 添加要插入的文档（书签操作方式是内容替换）
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param docPath 文档的路径（服务器上绝对路径）
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkDocument(String name,String docPath) throws JSONException, NoSuchAlgorithmException, IOException {
		addBookMarkDocument(name, docPath, BOOKMARK_OTYPE_REPLACE);
	}
	/**
	 * 添加要插入书签的内容
	 * @param name 书签名,书签名字必须以字母、汉字开头，不能以数字、英文符号、空格开头
	 * @param contentOrPath 书签内容 或者是 图片文档的文件服务器路径
	 * @param type 插入书签内容的类型，传递内容为BOOKMARK_TYPE_*
	 * @param operateType 操作方式，传递内容为BOOKMARK_OTYPE_*
	 * @param picWidth 图片类型的图片宽度，单位px
	 * @param picHeight 图片类型的图片高度，单位px
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void addBookMarkInfo(String name,String contentOrPath,int type,int operateType,int picWidth,int picHeight) throws JSONException, NoSuchAlgorithmException, IOException {
//		bookMarkJson
		if(name==null || "".equals(name.trim()) || contentOrPath==null) {
			System.out.println("待添加的书签名称为空，或数据为null");
			return;
		}
		int i=getIndex(name);
		JSONObject json;
		int len =bookMarkJson.length();
		if(i==-1) {
			json = new JSONObject();
		}else {
			if(i>=len) {
				throw new JSONException("数据发生错误，书签对应索引超出书签数组长度");
			}
			json = bookMarkJson.optJSONObject(i);
			if(json==null) {
				throw new JSONException("数据发生错误，书签对应数据为空");
			}
			if(!name.equals(json.optString("name"))){
				throw new JSONException("数据发生错误，书签对应数据书签名称不一致:【"+name+"】【"+json.optString("name")+"】");
			}
		}
		json.put("name", name);
		json.put("value", getValue(contentOrPath,type));
		json.put("type", type);
		json.put("optype", operateType);
		if(picWidth>0) {
			json.put("width", picWidth);
		}
		if(picHeight>0) {
			json.put("height", picHeight);
		}
		if(i==-1) {
			bookMarkJson.put(len, json);
			index.put(name, len);
		}
	}
	/**
	 * 根据类型获取内容
	 * @param contentOrPath
	 * @param type
	 * @return
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */
	private String getValue(String contentOrPath,int type) throws NoSuchAlgorithmException, JSONException, IOException {
		switch (type) {
		case BOOKMARK_TYPE_TXT:
			return contentOrPath;
		case BOOKMARK_TYPE_PIC:
			return MARK_FILE_PREFIX+upLoadFile(contentOrPath);
		case BOOKMARK_TYPE_DOCUMENT:
			return MARK_FILE_PREFIX+upLoadFile(contentOrPath);
		default:
			return contentOrPath;
		}
	}
	private String upLoadFile(String filePath) throws NoSuchAlgorithmException, JSONException, IOException {
    	Map<String, Object> requestBody = new HashMap<>();
    	requestBody.put("file", new File(filePath));
    	String response = Sender.post(ConvertParams.URL_CONVERT_FILE,ConvertParams.CONVERT_TYPE_CONVERT_FILE,requestBody);
    	JSONObject json = new JSONObject(response);
    	if(json.optInt("code")!=0) {
    		throw new IOException("上传文件失败："+json.optString("msg"));
    	}
        String inputPath =  json.optString("data");
        System.out.println("文件地址:"+inputPath);
        return inputPath;
	}
	/**
	 * 得到索引
	 * @param name
	 * @return
	 */
	private int getIndex(String name) {
		Integer i = index.get(name);
		if(i==null) {
			return -1;
		}else {
			return i;
		}
	}
	/**
	 * 将书签配置加到扩展参数
	 * @param extraParam
	 * @throws JSONException
	 */
	public void addExtraParam(JSONObject extraParam) throws JSONException {
		extraParam.put("bookMarkJson", bookMarkJson.toString());
	}
	public static void main(String[] args) {
	}
}
