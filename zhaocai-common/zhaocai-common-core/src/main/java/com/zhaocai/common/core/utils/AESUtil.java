package com.zhaocai.common.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * @description AES 加解密工具
 * @author ssy
 * @date 2024/9/14 10:53
 */
public class AESUtil {
    private static final Logger logger = LoggerFactory.getLogger(AESUtil.class);

    /** 密钥 */
    public static String key = "88420f686b5411efbdce0c42a181e696";
    /** 编码方式 */
    private static final String CHARSET = "utf-8";
    /** 加密器类型:加密算法为AES,加密模式为ECB,补码方式为PKCS5Padding */
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    /** 算法类型：用于指定生成AES的密钥 */
    private static final String ALGORITHM = "AES";

    /**
     * 加密
     */
    public static String encrypt(String content) {
        return encrypt(content, key);
    }

    /**
     * 解密
     */
    public static String decrypt(String content) {
        return decrypt(content, key);
    }

    /**
     * 加密
     *
     * @param content 需要加密的内容
     * @param key     加密密码
     * @return
     */
    public static String encrypt(String content, String key) {
        try {
            //构造密钥
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            //创建AES加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] byteContent = content.getBytes(CHARSET);
            //使用加密器的加密模式
            cipher.init(Cipher.ENCRYPT_MODE, sKey);
            // 加密
            byte[] result = cipher.doFinal(byteContent);
            //使用BASE64对加密后的二进制数组进行编码
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("{}", e);
        }
        return null;
    }

    /**
     * AES解密
     *
     * @param content 待解密内容
     * @param key     解密密钥
     * @return 解密之后
     * @throws Exception
     */
    public static String decrypt(String content, String key) {
        try {

            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            //解密时使用加密器的解密模式
            // 初始化
            cipher.init(Cipher.DECRYPT_MODE, sKey);
            byte[] result = cipher.doFinal(Base64.getMimeDecoder().decode(content));
            // 解密
            return new String(result);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("{}", e);
        }
        return null;
    }

    public static void main(String[] args) {
        String s = "湘AQ12580";
        String encryptResultStr = encrypt(s);
        // 加密
        System.out.println("加密前：" + s);
        System.out.println("加密后：" + encryptResultStr);
        // 解密
        System.out.println("解密后：" + decrypt(encryptResultStr));
        String base64Encode = Base64.getEncoder().encodeToString(s.getBytes());
        System.out.println("base64加密后：" + base64Encode);
        System.out.println("base64解密后：" + new String(Base64.getDecoder().decode(base64Encode)));
    }
}

