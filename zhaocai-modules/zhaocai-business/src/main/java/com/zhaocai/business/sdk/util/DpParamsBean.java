package com.zhaocai.business.sdk.util;

public class DpParamsBean {
	private String key;
	private String value;
	private int type=0;
	/**
	 * 字符串类型，默认字符串
	 */
	public static final int TYPE_STRING=0;
	/**
	 * 数值类型
	 */
	public static final int TYPE_NUMBER=1;
	/**
	 * 布尔类型
	 */
	public static final int TYPE_BOOLEAN=2;
	/**
	 * Json类型
	 */
	public static final int TYPE_JSONOBJECT=3;
	/**
	 * JsonArray类型
	 */
	public static final int TYPE_JSONARRAY=4;
	/**
	 * 构造函数
	 * @param key 键
	 * @param value 值
	 * @param type 类型，通过DpParamsBean TYPE_* 常亮进行设置
	 */
	public DpParamsBean(String key,String value,int type) {
		setKey(key);
		setValue(value);
		setType(type);
	}
	/**
	 * 构造函数，类型为字符串
	 * @param key 键
	 * @param value 值
	 */
	public DpParamsBean(String key,String value) {
		setKey(key);
		setValue(value);
		setType(TYPE_STRING);
	}
	/**
	 * 得到key
	 * @return
	 */
	public String getKey() {
		return key;
	}
	/**
	 * 设置key
	 * @param key
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * 得到值
	 * @return
	 */
	public String getValue() {
		switch (type) {
		case TYPE_STRING:
			return value;
		case TYPE_NUMBER:
			if(value==null || "".equals(value.trim())) {
				return "0";
			}
			if(Validation.isNumber(value)) {
				return value;
			}else {
				return "0";
			}
		case TYPE_BOOLEAN:
			if(value==null || !"true".equalsIgnoreCase(value.trim())) {
				return "false";
			}else {
				return "true";
			}
		case TYPE_JSONOBJECT:
			if(value==null || "".equals(value.trim())) {
				return "{}";
			}
			if(value.startsWith("{") && value.endsWith("}")) {
				return value;
			}else {
				throw new RuntimeException("数据格式不正确，不是JSON格式："+value);
			}
		case TYPE_JSONARRAY:
			if(value==null || "".equals(value.trim())) {
				return "[]";
			}
			if(value.startsWith("[") && value.endsWith("]")) {
				return value;
			}else {
				throw new RuntimeException("数据格式不正确，不是JSONArray格式："+value);
			}
		default:
			return value;
		}
	}
	/**
	 * 设置值
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/**
	 * 得到类型
	 * @return
	 */
	public int getType() {
		return type;
	}
	/**
	 * 设置类型
	 * @param type 通过DpParamsBean TYPE_* 常亮进行设置
	 */
	public void setType(int type) {
		this.type = type;
	}
	/**
	 * 是否带分隔符号，逗号。如果是最后一个，则不带逗号
	 * @param isSplit
	 * @return
	 */
	public String getJsonKVString(boolean isSplit) {
		String marks="\"";
		String colon=":";
		String split=",";
		StringBuffer sb = new StringBuffer();
		if(key==null || "".equals(key.trim())) {
			return "";
		}
		sb.append(marks).append(key).append(marks).append(colon);
		switch (type) {
		case TYPE_STRING:
			sb.append(marks).append(getValue().replaceAll("\"","\\\\\"")).append(marks);
			break;
		case TYPE_NUMBER:
			sb.append(getValue());
			break;
		case TYPE_BOOLEAN:
			sb.append(getValue());
			break;
		case TYPE_JSONOBJECT:
			sb.append(getValue());
			break;
		case TYPE_JSONARRAY:
			sb.append(getValue());
			break;
		default:
			sb.append(marks).append(getValue()).append(marks);
			break;
		}
		if(isSplit) {
			sb.append(split);
		}
		return sb.toString();
	}
	
}
