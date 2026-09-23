package com.zhaocai.business.sdk.bean;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class WaterMark {
	/**
	 * 宋体
	 */
	public static final String FONT_SONGTI="宋体";
	/**
	 * 黑体
	 */
	public static final String FONT_HEITI="黑体";
	/**
	 * 楷体
	 */
	public static final String FONT_KAITI="楷体";
	/**
	 * 隶书
	 */
	public static final String FONT_LISHU="隶书";
	/**
	 * 仿宋
	 */
	public static final String FONT_FANGSONG="仿宋";
	/**
	 * 小标宋
	 */
	public static final String FONT_XIAOBIAOSONG="方正小标宋简体";
	/**
	 * 文字水印
	 */
	public static final int TYPE_TXT=0;
	/**
	 * 图片水印
	 */
	public static final int TYPE_PIC=1;
	/**
	 * 十六进制颜色校验
	 */
	private static final Pattern HEX_PATTERN = Pattern.compile("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$");
	/**
	 * 文字水印内容
	 */
	String wmContent;
	/**
	 * 水印字体大小，单位px
	 */
	int wmSize=-1;
	/**
	 * 水印颜色，十六进制颜色，例如#808080
	 */
	String wmColor;
	/**
	 * 水印字体，服务器上部署的字体名称，例如：方正黑体简体
	 */
	String wmFont;
	/**
	 * 水印X轴间距
	 */
	int wmMarginX=-1;
	/**
	 * 水印Y轴间距
	 */
	int wmMarginY=-1;
	/**
	 * 水印开始位置左上角坐标X
	 */
	int wmPositionX=-1;
	/**
	 * 水印开始位置左上角坐标Y
	 */
	int wmPositionY=-1;
	/**
	 * 水印旋转角度 取值范围:（-180至 180）;默认-45°(逆时针 45度)，正数为顺时针旋转，负数为逆时针旋转
	 */
	int wmRotate=999;
	/**
	 * 水印不透明度 0-1之间的数值代表透明度  0完全透明 1完全不透明
	 */
	float wmTransparency=-1;
	/**
	 * 图片水印的路径
	 */
	String wmPicPath;
	/**
	 * 图片水印的图片base64
	 */
	String wmImage;
	/**
	 * 图片水印宽度
	 */
	int width=-1;
	/**
	 * 图片水印高度
	 */
	int height=-1;
	/**
	 * 构造函数
	 */
	public WaterMark() {
		setDefault();
	}
	/**
	 * @param type 水印类型 0 文字水印 1图片水印（编辑不支持图片水印）
	 * @param wmPicPath 图片水印在服务器上的绝对路径，文字水印的内容
	 * @throws IOException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public WaterMark(int type,String contentOrPicPath) throws IOException, NoSuchAlgorithmException, JSONException {
		setDefault();
		switch (type) {
		case TYPE_TXT:
			setWmContent(contentOrPicPath);
			break;
		case TYPE_PIC:
			//如果是http开头的，则是网络文件
			String path = contentOrPicPath.toLowerCase();
			if(path.startsWith("http://") || path.startsWith("https://")) {
				setWmPicPath(contentOrPicPath);
			}else {
				setWmPicBase64(contentOrPicPath);
			}
			break;
		default:
			setWmContent(contentOrPicPath);
			break;
		}
	}
	/**
	 * 设置水印默认参数
	 */
	private void setDefault() {
		//TODO 请修改构造函数，在这里设置水印的默认参数，方便系统中统一水印
		setWmColor("#D3D3D3");
		setWmFont(FONT_SONGTI);
		setWmSize(24);
		setWmMarginX(100);
		setWmMarginY(100);
		setWidth(100);
		setHeight(100);
		setWmRotate(-30);
		setWmTransparency(0.7f);
	}
	/**
	 * 得到文字水印的内容
	 * @return wmContent 文字水印的内容
	 */
	public String getWmContent() {
		return wmContent;
	}
	/**
	 * 设置文字水印内容
	 * @param wmContent 文字水印的内容
	 */
	public void setWmContent(String wmContent) {
		this.wmContent = wmContent;
	}
	/**
	 * 得到文字水印字体大小
	 * @return wmSize 文字水印字体大小
	 */
	public int getWmSize() {
		return wmSize;
	}
	/**
	 * 设置文字水印字体大小，单位px
	 * @param wmSize 文字水印字体大小
	 */
	public void setWmSize(int wmSize) {
		this.wmSize = wmSize;
	}
	/**
	 * 得到文字水印字体颜色
	 * @return wmColor 文字水印字体颜色
	 */
	public String getWmColor() {
		return wmColor;
	}
	/**
	 * 文字水印字体颜色，十六进制颜色，例如#808080
	 * @param wmColor 十六进制颜色
	 */
	public void setWmColor(String wmColor) {
		if (wmColor == null) {
            throw new RuntimeException("文字水印字体颜色设置为null了，如果为默认值请不要设置，应为十六进制颜色，例如#D3D3D3");
        }
        // 移除字符串首尾的空白字符
		wmColor = wmColor.trim();
        // 检查是否为十六进制颜色代码
        if (HEX_PATTERN.matcher(wmColor).matches()) {
        	this.wmColor = wmColor;
        }else {
        	throw new RuntimeException("文字水印字体颜色错误，应为十六进制颜色，例如#D3D3D3");
        }
	}
	/**
	 * 得到文字水印字体
	 * @return wmFont
	 */
	public String getWmFont() {
		return wmFont;
	}
	/**
	 * 设置文字水印字体
	 * @param wmFont 字体，请不要直接手工输入字体，在WaterMark.FONT_常量中选择
	 */
	public void setWmFont(String wmFont) {
		this.wmFont = wmFont;
	}
	/**
	 * 得到水印X轴间距
	 * @return wmMarginX 水印X轴间距
	 */
	public int getWmMarginX() {
		return wmMarginX;
	}
	/**
	 * 设置水印X轴间距
	 * @param wmMarginX 水印X轴间距
	 */
	public void setWmMarginX(int wmMarginX) {
		this.wmMarginX = wmMarginX;
	}
	/**
	 * 得到水印Y轴间距
	 * @return wmMarginY 水印Y轴间距
	 */
	public int getWmMarginY() {
		return wmMarginY;
	}
	/**
	 * 设置水印Y轴间距
	 * @param wmMarginY 水印Y轴间距
	 */
	public void setWmMarginY(int wmMarginY) {
		this.wmMarginY = wmMarginY;
	}
	/**
	 * 得到水印开始位置左上角坐标X
	 * @return wmPositionX 水印开始位置左上角坐标X
	 */
	public int getWmPositionX() {
		return wmPositionX;
	}
	/**
	 * 设置水印开始位置左上角坐标X，如果需要平铺水印，请不要设置此参数
	 * @param wmPositionX 水印开始位置左上角坐标X
	 */
	public void setWmPositionX(int wmPositionX) {
		this.wmPositionX = wmPositionX;
	}
	/**
	 * 得到水印开始位置左上角坐标Y
	 * @return wmPositionY 水印开始位置左上角坐标Y
	 */
	public int getWmPositionY() {
		return wmPositionY;
	}
	/**
	 * 设置水印开始位置左上角坐标Y，如果需要平铺水印，请不要设置此参数
	 * @param wmPositionY 水印开始位置左上角坐标Y
	 */
	public void setWmPositionY(int wmPositionY) {
		this.wmPositionY = wmPositionY;
	}
	/**
	 * 得到水印旋转角度
	 * @return wmRotate 水印旋转角度
	 */
	public int getWmRotate() {
		return wmRotate;
	}
	/**
	 * 设置水印旋转角度
	 * @param wmRotate 水印旋转角度 取值范围:（-180至 180）;默认-45°(逆时针 45度)，正数为顺时针旋转，负数为逆时针旋转
	 */
	public void setWmRotate(int wmRotate) {
		if(wmRotate<-180) {
			wmRotate=-180;
		}
		if(wmRotate>180) {
			wmRotate=180;
		}
		this.wmRotate = wmRotate;
	}
	/**
	 * 得到水印不透明度
	 * @return wmTransparency 水印不透明度 0-1之间的数值代表透明度  0完全透明 1完全不透明
	 */
	public float getWmTransparency() {
		return wmTransparency;
	}
	/**
	 * 设置水印不透明度
	 * @param wmTransparency 水印不透明度 0-1之间的数值代表透明度  0完全透明 1完全不透明
	 */
	public void setWmTransparency(float wmTransparency) {
		if(wmTransparency>1) {
			wmTransparency=1;
		}
		if(wmTransparency<0) {
			wmTransparency=0;
		}
		this.wmTransparency = wmTransparency;
	}
	/**
	 * 得到图片水印的路径
	 * @return wmPicPath 如果没有图片水印，则为null
	 */
	public String getWmPicPath() {
		return wmPicPath;
	}
	/**
	 * 得到图片的base64
	 * @return
	 */
	public String getWmPicBase64() {
		return wmImage;
	}
	/**
	 * 设置图片水印内容(调试中)
	 * @param wmPicPath 图片水印的路径
	 * @throws IOException
	 */
	public void setWmPicBase64(String wmPicPath) throws IOException {
		if(wmPicPath==null || "".equals(wmPicPath.trim())) {
			throw new IOException("图片水印路径为空");
		}
		Path filePath = Paths.get(wmPicPath);
        byte[] fileBytes = Files.readAllBytes(filePath);
        String base64EncodedString = Base64.getEncoder().encodeToString(fileBytes);
		this.wmImage = base64EncodedString;
		this.wmPicPath=null;
	}
	/**
	 * 设置图片水印内容
	 * @param wmPicPath 可以访问的图片地址
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws IOException
	 */
	public void setWmPicPath(String wmPicPath) throws NoSuchAlgorithmException, JSONException, IOException {
//		//组织要上传的图片水印参数
//    	Map<String, Object> requestBody = new HashMap<>();
//    	requestBody.put("file", new File(wmPicPath));
//    	String response = Sender.post(Sender.URL_CONVERT_FILE,Sender.CONVERT_TYPE_CONVERT_FILE,requestBody);
//    	JSONObject json = new JSONObject(response);
//    	if(json.optInt("code")!=0) {
//    		throw new IOException("上传水印图片失败："+json.optString("msg"));
//    	}
//        String inputPath =  json.optString("data");
//        System.out.println("水印图片地址:"+inputPath);
//        this.wmPicBase64=inputPath;
		this.wmPicPath=wmPicPath;
		this.wmImage =null;
	}
	/**
	 * 得到图片水印的宽度，单位px
	 * @return width 水印图片宽度
	 */
	public int getWidth() {
		return width;
	}
	/**
	 * 设置图片水印的宽度
	 * @param width 水印图片宽度，单位px
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	/**
	 * 得到图片水印的高度，单位px
	 * @return height 水印图片高度
	 */
	public int getHeight() {
		return height;
	}
	/**
	 * 设置图片水印的高度
	 * @param height 水印图片高度，单位px
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	/**
	 * 将水印属性追加到扩展参数中
	 * @param extraParam 扩展参数
	 * @throws JSONException
	 */
	protected void addExtraParam(JSONObject extraParam) throws JSONException {
		if(getWmContent()!=null) {
			extraParam.put("wmContent", getWmContent());
		}
		if(getWmSize()!=-1) {
			extraParam.put("wmSize", getWmSize());
		}
		if(getWmColor()!=null) {
			extraParam.put("wmColor", getWmColor());
		}
		if(getWmFont()!=null) {
			extraParam.put("wmFont", getWmFont());
		}
		if(getWmMarginX()!=-1 || getWmMarginY()!=-1) {
			if(getWmMarginX()==-1) {
				setWmMarginX(0);
			}
			if(getWmMarginY()==-1) {
				setWmMarginY(0);
			}
			extraParam.put("wmMargin", getWmMarginX()+","+getWmMarginY());
		}
		if(getWmTransparency()!=-1) {
			extraParam.put("wmTransparency", getWmTransparency());
		}
		if(getWmRotate()!=999) {
			extraParam.put("wmRotate", getWmRotate());
		}
		if(getWmPositionX()!=-1 || getWmPositionY()!=-1) {
			if(getWmPositionX()==-1) {
				setWmPositionX(0);
			}
			if(getWmPositionY()==-1) {
				setWmPositionY(0);
			}
			extraParam.put("wmPosition", getWmPositionX()+","+getWmPositionY());
		}
		if(getWidth()!=-1 || getHeight()!=-1) {
			if(getWidth()==-1) {
				setWidth(0);
			}
			if(getHeight()==-1) {
				setHeight(0);
			}
			extraParam.put("wmPicSize", getWidth()+","+getHeight());
		}
		if(getWmPicBase64()!=null) {
			extraParam.put("wmImage", getWmPicBase64());
		}
		if(getWmPicPath()!=null) {
			extraParam.put("wmPicPath", getWmPicPath());
		}
	}
	/**
	 * 将水印追加到编辑的扩展参数中
	 * @param extraParamWaterMark
	 * @throws JSONException
	 */
	protected void addEditExtraParam(JSONObject extraParamWaterMark) throws JSONException {
		if(getWmContent()!=null) {
			extraParamWaterMark.put("Text", getWmContent());
		}
		if(getWmSize()!=-1) {
			extraParamWaterMark.put("FontSize", getWmSize());
		}
		if(getWmColor()!=null) {
			extraParamWaterMark.put("Color", getWmColor());
		}
		if(getWmFont()!=null) {
			extraParamWaterMark.put("FontFamily", getWmFont());
		}
		if(getWmTransparency()!=-1) {
			extraParamWaterMark.put("FontOpacity", getWmTransparency());
		}
		if(getWmRotate()!=999) {
			extraParamWaterMark.put("FontRotate", getWmRotate());
		}
		if(getWmPositionX()!=-1) {
			extraParamWaterMark.put("FontWidth", getWmPositionX());
		}
		if(getWmPositionY()!=-1) {
			extraParamWaterMark.put("FontHeight", getWmPositionY());
		}
	}
}
