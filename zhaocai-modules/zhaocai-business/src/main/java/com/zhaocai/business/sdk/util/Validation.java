package com.zhaocai.business.sdk.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;  

 
/**
 * 验证工具类
 * @author admin
 *
 */
public class Validation {  
	private static Map<String, String> methodTypeNames ;
	static {
		/**
		 * * 数据类型，可以包含以下类型，多个类型以逗号分隔：
     * <br><b>电子邮件：</b>Email;
     * <br><b>电话：</b>Phone;<b>手机：</b>Mobile;
     * <br><b>网址：</b>Url;
     * <br><b>整数：</b>Integer;<b>整数：</b>Int;<b>正整数：</b>PositiveInteger;<b>负整数：</b>NegtiveInteger;
     * <br><b>数字：</b>Number;<b>浮点数：</b>Double;
     * <br><b>正浮点数：</b>PositiveDouble;<b>负浮点数：</b>NegtiveDouble;
     * <br><b>日期（yyyy-MM-dd）：</b>Date;<b>日期时间(yyyy-MM-dd HH:mm:ss):</b>DateTime
     * <br><b>年龄：</b>Age;<b>身份证：</b>IdCard;<b>邮政编码：</b>PostCode;
     * <br><b>中文：</b>Chinese;<b>英文：</b>English;
     * <br><b>英文和数字：</b>EnglishNumber;<b>英文数字和下划线：</b>EnglishNumber_;
     * <br><b>生日：</b>Birthday;<b>ip地址：</b>IpV4
		 */
		methodTypeNames=new HashMap<String,String>();
		methodTypeNames.put("isEmail", "电子邮件");
		methodTypeNames.put("isPhone", "电话号码");
		methodTypeNames.put("isMobile", "手机号码");
		methodTypeNames.put("isUrl", "网址");
		methodTypeNames.put("isInteger", "整数");
		methodTypeNames.put("isInt", "整数");
		methodTypeNames.put("isPositiveInteger", "正整数");
		methodTypeNames.put("isNegtiveInteger", "负整数");
		methodTypeNames.put("isNumber", "数字");
		methodTypeNames.put("isDouble", "浮点数");
		methodTypeNames.put("isPositiveDouble", "正浮点数");
		methodTypeNames.put("isNegtiveDouble", "负浮点数");
		methodTypeNames.put("isOrderPrice", "有效订单金额");
		methodTypeNames.put("isDate", "日期");
		methodTypeNames.put("isDateTime", "日期时间");
		methodTypeNames.put("isAge", "年龄");
		methodTypeNames.put("isIdCard", "身份证");
		methodTypeNames.put("isPostCode", "邮政编码");
		methodTypeNames.put("isChinese", "中文");
		methodTypeNames.put("isEnglish", "英文字母");
		methodTypeNames.put("isEnglishNumber", "英文字母和数字");
		methodTypeNames.put("isEnglishNumber_", "英文字母、数字和下划线");
		methodTypeNames.put("isBirthday", "生日");
		methodTypeNames.put("isIpV4", "IP地址");
	}
    //------------------常量定义  
    /** 
     * Email正则表达式="\\w+(\\.\\w+)*@\\w+(\\.\\w+)+";
     */
    //public static final String EMAIL = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";; 
//	 "\\w+@\\w+\\.[a-z]+(\\.[a-z]+)?"; 
    public static final String EMAIL = "\\w+(\\.\\w+)*@\\w+(\\.\\w+)+";
    /** 
     * 电话号码正则表达式= (^(\d{2,4}[-_－—]?)?\d{3,8}([-_－—]?\d{3,8})?([-_－—]?\d{1,7})?$)|(^0?1[35]\d{9}$)  
     */
    public static final String PHONE = "(^(\\d{2,4}[-_－—]?)?\\d{3,8}([-_－—]?\\d{3,8})?([-_－—]?\\d{1,7})?$)|(^0?1[35]\\d{9}$)" ;  
    /** 
     * 手机号码正则表达式=^(13[0-9]|14[0-9]|15[0-9]|17[0-9]|18[0-9])\d{8}$
     */
//    public static final String MOBILE ="^(13[0-9]|14[0-9]|15[0-9]|17[0-9]|18[0-9])\\d{8}$";  
    public static final String MOBILE ="(\\+\\d+)?1[3456789]\\d{9}$";  
    /** 
     * Integer正则表达式 ^-?(([1-9]\d*$)|0) 
     */
    public static final String  INTEGER = "^-?(([1-9]\\d*$)|0)";  
    /** 
     * 正整数正则表达式 >=0 ^[1-9]\d*|0$ 
     */
    public static final String  INTEGER_POSITIVE = "^[1-9]\\d*|0$";  
    /** 
     * 负整数正则表达式 <=0 ^-[1-9]\d*|0$ 
     */
    public static final String  INTEGER_NEGATIVE = "^-[1-9]\\d*|0$";      
    /**
     * 订单金额
     */
    public static final String  ORDER_PRICE = "^(([1-9]{1}\\d*)|([0]{1}))(\\.(\\d){0,2})?$";
    /** 
     * Double正则表达式 ^-?([1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0)$ 
     */
    public static final String  DOUBLE ="^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$";  
    /** 
     * 正Double正则表达式 >=0  ^[1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0$　 
     */
    public static final String  DOUBLE_POSITIVE ="^[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0$";  
    /** 
     * 负Double正则表达式 <= 0  ^(-([1-9]\d*\.\d*|0\.\d*[1-9]\d*))|0?\.0+|0$ 
     */
    public static final String  DOUBLE_NEGATIVE ="^(-([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*))|0?\\.0+|0$";   
    /** 
     * 年龄正则表达式 ^(?:[1-9][0-9]?|1[01][0-9]|120)$ 匹配0-120岁 
     */
    public static final String  AGE="^(?:[1-9][0-9]?|1[01][0-9]|120)$";  
    /** 
     * 邮编正则表达式  [0-9]\d{5}(?!\d) 国内6位邮编 
     */
    public static final String  CODE="[0-9]\\d{5}(?!\\d)";    
    /** 
     * 匹配由数字、26个英文字母或者下划线组成的字符串 ^\w+$ 
     */
    public static final String STR_ENG_NUM_="^\\w+$";  
    /** 
     * 匹配由数字和26个英文字母组成的字符串 ^[A-Za-z0-9]+$  
     */
    public static final String STR_ENG_NUM="^[A-Za-z0-9]+";  
    /** 
     * 匹配由26个英文字母组成的字符串  ^[A-Za-z]+$ 
     */
    public static final String STR_ENG="^[A-Za-z]+$";  
    /** 
     * 过滤特殊字符串正则 
     * regEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";  
     */
    public static final String STR_SPECIAL="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";  
    /*** 
     * 日期正则 支持： 
     *  YYYY-MM-DD  
     *  YYYY/MM/DD  
     *  YYYY_MM_DD  
     *  YYYYMMDD 
     *  YYYY.MM.DD的形式 
     */
    public static final String DATE_ALL="((^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(10|12|0?[13578])([-\\/\\._]?)(3[01]|[12][0-9]|0?[1-9])$)" +  
            "|(^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(11|0?[469])([-\\/\\._]?)(30|[12][0-9]|0?[1-9])$)" +  
            "|(^((1[8-9]\\d{2})|([2-9]\\d{3}))([-\\/\\._]?)(0?2)([-\\/\\._]?)(2[0-8]|1[0-9]|0?[1-9])$)|(^([2468][048]00)([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([3579][26]00)" +  
            "([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)" +  
            "|(^([1][89][0][48])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([2-9][0-9][0][48])([-\\/\\._]?)" +  
            "(0?2)([-\\/\\._]?)(29)$)" +  
            "|(^([1][89][2468][048])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|(^([2-9][0-9][2468][048])([-\\/\\._]?)(0?2)" +  
            "([-\\/\\._]?)(29)$)|(^([1][89][13579][26])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$)|" +  
            "(^([2-9][0-9][13579][26])([-\\/\\._]?)(0?2)([-\\/\\._]?)(29)$))";  
    /*** 
     * 日期正则 支持： 
     *  YYYY-MM-DD  
     */
    public static final String DATE_FORMAT1="(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)";
     
    /** 
     * URL正则表达式 
      * 匹配 http www ftp https
     */
    public static final String URL = "^(http|https|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?" +  
                                    "(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*" +  
                                    "(\\w*:)*(\\w*\\+)*(\\w*\\.)*" +  
                                    "(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";   
  
    /** 
     * 身份证正则表达式 
     */
    public static final String IDCARD="((11|12|13|14|15|21|22|23|31|32|33|34|35|36|37|41|42|43|44|45|46|50|51|52|53|54|61|62|63|64|65)[0-9]{4})" +  
                                        "(([1|2][0-9]{3}[0|1][0-9][0-3][0-9][0-9]{3}" +  
                                        "[Xx0-9])|([0-9]{2}[0|1][0-9][0-3][0-9][0-9]{3}))";
     
    /**
     * 机构代码
     */
    public static final String JIGOU_CODE = "^[A-Z0-9]{8}-[A-Z0-9]$";
     
    /**
     * 匹配数字组成的字符串  ^[0-9]+$ 
     */
    public static final String STR_NUM = "^[0-9]+$";  
       
////------------------验证方法       
    /** 
     * 判断字段是否为空 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static synchronized boolean StrisNull(String str) {  
        return null == str || str.trim().length() <= 0 ? true : false ;  
    }  
    /** 
     * 判断字段是非空 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNotNull(String str) {  
        return !StrisNull(str) ;  
    }  
    /** 
     * 字符串null转空 
     * @param str 
     * @return boolean 
     */
    public static  String nulltoStr(String str) {
        return StrisNull(str)?"":str;  
    }     
    /** 
     * 字符串null赋值默认值  
     * @param str    目标字符串 
     * @param defaut 默认值 
     * @return String 
     */
    public static  String nulltoStr(String str,String defaut) {  
        return StrisNull(str)?defaut:str;  
    }  
    /** 
     * 判断字段是否为Email 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isEmail(String str) {  
        return Regular(str,EMAIL);  
    }  
    /** 
     * 判断是否为电话号码 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPhone(String str) {  
        return Regular(str,PHONE);  
    }  
    /** 
     * 判断是否为手机号码 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isMobile(String str) {  
        return Regular(str,MOBILE);  
    }  
    /** 
     * 判断是否为Url 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isUrl(String str) {  
        return Regular(str,URL);  
    }     
    /**  
     * 判断字段是否为数字 正负整数 正负浮点数 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNumber(String str) {  
        return isDouble(str) || isInteger(str);  
    }  
    /** 
     * 判断字段是否为INTEGER  符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isInteger(String str) {  
        return Regular(str,INTEGER);  
    }  
    public static  boolean isInt(String str) {  
    	return Regular(str,INTEGER);  
    }  
    /** 
     * 判断字段是否为正整数正则表达式 >=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPositiveInteger(String str) {  
        return Regular(str,INTEGER_POSITIVE);  
    }  
    /** 
     * 判断字段是否为负整数正则表达式 <=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNegtiveInteger(String str) {  
        return Regular(str,INTEGER_NEGATIVE);  
    }     
    /**
     * 判断是否是有效的订单金额，2位小数以内的正数，0也是有效
     * @param str
     * @return
     */
    public static  boolean isOrderPrice(String str) {  
    	return Regular(str,ORDER_PRICE);  
    }     
    /** 
     * 判断字段是否为DOUBLE 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isDouble(String str) {  
        return Regular(str,DOUBLE);  
    }  
    /**  
     * 判断字段是否为正浮点数正则表达式 >=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPositiveDouble(String str) {  
        return Regular(str,DOUBLE_POSITIVE);  
    }  
    /** 
     * 判断字段是否为负浮点数正则表达式 <=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNegtiveDouble(String str) {  
        return Regular(str,DOUBLE_NEGATIVE);  
    }     
    /** 
     * 判断字段是否为日期 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isDate(String str) {  
        return Regular(str,DATE_FORMAT1);  
    }  
    /**
     * 判断字段是否是日期时间格式
     * @param str
     * @return
     */
    public static boolean isDateTime(String str) {
        boolean convertSuccess = true;
        // 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // 设置lenient为false.
            // 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            // e.printStackTrace();
            // 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
            convertSuccess = false;
        }
        return convertSuccess;
    }
//    /**
//     * 验证2010-12-10
//     * @param str
//     * @return
//     */
//    public static  boolean isDate1(String str) {  
//        return Regular(str,DATE_FORMAT1);  
//    }  
    /** 
     * 判断字段是否为年龄 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isAge(String str) {  
        return Regular(str,AGE) ;  
    }  
    /** 
     * 判断字段是否超长 
     * 字串为空返回fasle, 超过长度{leng}返回ture 反之返回false 
     * @param str 
     * @param leng 
     * @return boolean 
     */
    public static  boolean isLengOut(String str,int leng) {       
        return StrisNull(str)?false:str.trim().length() > leng ;  
    }  
    /** 
     * 判断字段是否为身份证 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isIdCard(String str) {  
//        if(StrisNull(str)) return false ;  
//        if(str.trim().length() == 15 || str.trim().length() == 18) {  
//                return Regular(str,IDCARD);  
//        }else {  
//            return false ;  
//        }  
           return isLegalPattern(str);
    }  
 // 身份证校验码
    private static final int[] COEFFICIENT_ARRAY = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    // 身份证号的尾数规则
    private static final String[] IDENTITY_MANTISSA = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

    private static final String IDENTITY_PATTERN = "^[0-9]{17}[0-9Xx]$";

    private static boolean isLegalPattern(String identity) {
        if (identity == null) {
            return false;
        }

        if (identity.length() != 18) {
            return false;
        }

        if (!identity.matches(IDENTITY_PATTERN)) {
            return false;
        }

        char[] chars = identity.toCharArray();
        long sum = IntStream.range(0, 17).map(index -> {
            char ch = chars[index];
            int digit = Character.digit(ch, 10);
            int coefficient = COEFFICIENT_ARRAY[index];
            return digit * coefficient;
        }).summaryStatistics().getSum();

        // 计算出的尾数索引
        int mantissaIndex = (int) (sum % 11);
        String mantissa = IDENTITY_MANTISSA[mantissaIndex];

        String lastChar = identity.substring(17);
        if (lastChar.equalsIgnoreCase(mantissa)) {
            return true;
        } else {
            return false;
        }
    }
//    /** 
//     * 判断字段是否为邮编 符合返回ture 
//     * @param str 
//     * @return boolean 
//     */
//    public static boolean isCode(String str) {  
//        return Regular(str,CODE) ;  
//    }  
    /**
	 * 匹配中国邮政编码
	 * @param postcode 邮政编码
	 * @return 验证成功返回true，验证失败返回false
	 */ 
	public static boolean isPostCode(String postcode) { 
	    String regex = "[1-9]\\d{5}"; 
	    return Pattern.matches(regex, postcode); 
	}
	/**
	 * 验证中文
	 * @param chinese 中文字符
	 * @return 验证成功返回true，验证失败返回false
	 */ 
	public static boolean isChinese(String chinese) { 
	    String regex = "^[\u4E00-\u9FA5]+$"; 
	    return Pattern.matches(regex,chinese); 
	}
	/** 
     * 判断字符串是不是全部是英文字母 
     * @param str 
     * @return boolean 
     */
    public static boolean isEnglish(String str) {  
        return Regular(str,STR_ENG) ;  
    }  
    /** 
     * 判断字符串是不是全部是英文字母+数字 
     * @param str 
     * @return boolean 
     */
    public static boolean isEnglishNumber(String str) {  
        return Regular(str,STR_ENG_NUM) ;  
    }  
    /** 
     * 判断字符串是不是全部是英文字母+数字+下划线 
     * @param str 
     * @return boolean 
     */
    public static boolean isEnglishNumber_(String str) {  
        return Regular(str,STR_ENG_NUM_) ;  
    }  
    /** 
     * 过滤特殊字符串 返回过滤后的字符串 
     * @param str 
     * @return boolean 
     */
    public static  String filterStr(String str) {  
        Pattern p = Pattern.compile(STR_SPECIAL);  
        Matcher m = p.matcher(str);  
        return   m.replaceAll("").trim();  
    }
     
    /**
     * 校验机构代码格式
     * @return
     */
    public static boolean isJigouCode(String str){
        return Regular(str,JIGOU_CODE) ;  
    }
    /**
     * 验证整数和浮点数（正负整数和正负浮点数）
     * @param decimals 一位或多位0-9之间的浮点数，如：1.23，233.30
     * @return 验证成功返回true，验证失败返回false
     */ 
    public static boolean isDecimals(String decimals) { 
        String regex = "\\-?[1-9]\\d+(\\.\\d+)?"; 
        return Pattern.matches(regex,decimals); 
    }  
     
    /**
     * 验证空白字符
     * @param blankSpace 空白字符，包括：空格、\t、\n、\r、\f、\x0B
     * @return 验证成功返回true，验证失败返回false
     */ 
    public static boolean isBlankSpace(String blankSpace) { 
        String regex = "\\s+"; 
        return Pattern.matches(regex,blankSpace); 
    } 
     
    /**
     * 验证日期（年月日）
     * @param birthday 日期，格式：1992-09-03，或1992.09.03
     * @return 验证成功返回true，验证失败返回false
     */ 
    public static boolean isBirthday(String birthday) { 
        String regex = "[1-9]{4}([-./])\\d{1,2}\\1\\d{1,2}"; 
        return Pattern.matches(regex,birthday); 
    } 
    /**
     * 匹配IP地址(简单匹配，格式，如：192.168.1.1，127.0.0.1，没有匹配IP段的大小)
     * @param ipAddress IPv4标准地址
     * @return 验证成功返回true，验证失败返回false
     */ 
    public static boolean isIpV4(String ipAddress) { 
        String regex = "[1-9](\\d{1,2})?\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))\\.(0|([1-9](\\d{1,2})?))"; 
        return Pattern.matches(regex, ipAddress); 
    }
    /** 
     * 判断字符串是不是数字组成 
     * @param str 
     * @return boolean 
     */
    public static boolean isSTR_NUM(String str) {  
        return Regular(str,STR_NUM) ;  
    } 
     
     
//    /**
//     * 验证身份证号码
//     * @param idCard 居民身份证号码15位或18位，最后一位可能是数字或字母
//     * @return 验证成功返回true，验证失败返回false
//     */ 
//    public static boolean checkIdCard(String idCard) { 
//        String regex = "[1-9]\\d{13,16}[a-zA-Z0-9]{1}"; 
//        return Pattern.matches(regex,idCard); 
//    } 
     
//    /**
//     * 验证手机号码（支持国际格式，+86135xxxx...（中国内地），+00852137xxxx...（中国香港））
//     * @param mobile 移动、联通、电信运营商的号码段
//     *<p>移动的号段：134(0-8)、135、136、137、138、139、147（预计用于TD上网卡）
//     *、150、151、152、157（TD专用）、158、159、187（未启用）、188（TD专用）</p>
//     *<p>联通的号段：130、131、132、155、156（世界风专用）、185（未启用）、186（3g）</p>
//     *<p>电信的号段：133、153、180（未启用）、189</p>
//     * @return 验证成功返回true，验证失败返回false
//     */ 
//    public static boolean checkMobile(String mobile) { 
//        String regex = "(\\+\\d+)?1[34578]\\d{9}$"; 
//        return Pattern.matches(regex,mobile); 
//    } 
//     
//    /**
//     * 验证固定电话号码
//     * @param phone 电话号码，格式：国家（地区）电话代码 + 区号（城市代码） + 电话号码，如：+8602085588447
//     * <p><b>国家（地区） 代码 ：</b>标识电话号码的国家（地区）的标准国家（地区）代码。它包含从 0 到 9 的一位或多位数字，
//     *  数字之后是空格分隔的国家（地区）代码。</p>
//     * <p><b>区号（城市代码）：</b>这可能包含一个或多个从 0 到 9 的数字，地区或城市代码放在圆括号——
//     * 对不使用地区或城市代码的国家（地区），则省略该组件。</p>
//     * <p><b>电话号码：</b>这包含从 0 到 9 的一个或多个数字 </p>
//     * @return 验证成功返回true，验证失败返回false
//     */ 
//    public static boolean checkPhone(String phone) { 
//        String regex = "(\\+\\d+)?(\\d{3,4}\\-?)?\\d{7,8}$"; 
//        return Pattern.matches(regex, phone); 
//    } 
     
//    /**
//     * 验证整数（正整数和负整数）
//     * @param digit 一位或多位0-9之间的整数
//     * @return 验证成功返回true，验证失败返回false
//     */ 
//    public static boolean checkDigit(String digit) { 
//        String regex = "\\-?[1-9]\\d+"; 
//        return Pattern.matches(regex,digit); 
//    } 
     

     
//    /**
//     * 验证URL地址
//     * @param url 格式：http://blog.csdn.net:80/xyang81/article/details/7705960? 或 http://www.csdn.net:80
//     * @return 验证成功返回true，验证失败返回false
//     */ 
//    public static boolean checkURL(String url) { 
//        String regex = "(https?://(w{3}\\.)?)?\\w+\\.\\w+(\\.[a-zA-Z]+)*(:\\d{1,5})?(/\\w*)*(\\??(.+=.*)?(&.+=.*)?)?"; 
//        return Pattern.matches(regex, url); 
//    } 
    
    /**
     * <pre>
     * 获取网址 URL 的一级域
     * </pre>
     * 
     * @param url
     * @return
     */
    public static String getDomain(String url) {
        Pattern p = Pattern.compile("(?<=http://|\\.)[^.]*?\\.(com|cn|net|org|biz|info|cc|tv)", Pattern.CASE_INSENSITIVE);
        // 获取完整的域名
        // Pattern p=Pattern.compile("[^//]*?\\.(com|cn|net|org|biz|info|cc|tv)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(url);
        matcher.find();
        return matcher.group();
    }
 
    
    
    
    
    /** 
     * 匹配是否符合正则表达式pattern 匹配返回true 
     * @param str 匹配的字符串 
     * @param pattern 匹配模式 
     * @return boolean 
     */
    private static  boolean Regular(String str,String pattern){  
        if(null == str || str.trim().length()<=0)  
            return false;           
        Pattern p = Pattern.compile(pattern);  
        Matcher m = p.matcher(str);  
        return m.matches();  
    }  
    /**
     * 校验的名称
     * @param method
     * @return
     */
    public static String ValidTypeName(String method){
    	return methodTypeNames.get(method);
    }
    public static void main(String[] args) {
		System.out.println(isSTR_NUM("45413165602000001"));
	}
  /**
   * 判断参数中有没有sql关键字
   * @param str
   * @return
   */
    public static boolean sqlValidate(String str) {
        str = str.toLowerCase();//统一转为小写
        String badStr ="'щandщexecщexecuteщinsertщselectщdeleteщupdateщcountщdropщ*щ%щchrщmidщmasterщtruncateщcharщdeclareщsitenameщnet userщxp_cmdshellщ;щorщ-щ+щ,щlike'щandщexecщexecuteщinsertщcreateщdropщtableщfromщgrantщuseщgroup_concatщcolumn_nameщinformation_schema.columnsщtable_schemaщunionщwhereщselectщdeleteщupdateщorderщbyщcountщ*щchrщmidщmasterщtruncateщcharщdeclareщorщ;щ-щ--щ+щ,щlikeщ//щ/щ%щ#щ";//过滤掉的sql关键字，可以手动添加
        if(badStr.indexOf("щ"+str+"щ")>=0) {
        	return true;
        }else {
        	return false;
        }
    }
}