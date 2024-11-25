package com.zhaocai.business.sdk.util;

import java.util.List;

public class JSONUtils {
	/**
	 * 将参数转换为json格式字符串
	 * @param params
	 * @return
	 */
	public static String toJsonString(List<DpParamsBean> params){
		int len = params.size();
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		for (int i = 0; i < len; i++) {
			DpParamsBean p = params.get(i);
			boolean isSplit=true;
			if(i==len-1) {
				isSplit=false;
			}
			sb.append(p.getJsonKVString(isSplit));
		}
		sb.append("}");
		return sb.toString();
	}
}
