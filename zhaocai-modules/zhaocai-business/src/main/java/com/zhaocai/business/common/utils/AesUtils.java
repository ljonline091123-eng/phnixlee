package com.zhaocai.business.common.utils;

import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesUtils {

    public static final String IV = "1234876556784321";

    /**
     * 算法的名称
     */
    private static final String AES = "AES";

    /**
     * 默认 AES/CBC/PKCS5Padding
     * <p>
     * 算法：AES
     * 模式：CBC； 其中CBC、CFB模式需要向量；OFB模式不需要向量
     * 填充：PKCS5Padding
     */
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * 编码 utf-8
     */
    private static final String UTF_8 = "utf-8";

    /**
     * 加密
     *
     * @param needEncryptStr 待加密字符串
     * @param key            加密key
     * @return
     * @throws Exception
     */
    public static String encrypt(String needEncryptStr, String key) throws Exception {
        byte[] raw = key.getBytes(UTF_8);
        //设置秘钥
        SecretKeySpec keySpec = new SecretKeySpec(raw, AES);
        //设置向量
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        //初始化加密方式  Cipher.ENCRYPT_MODE 加密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        //加密；   设置为utf-8, 防止中文和英文混合
        byte[] encrypted = cipher.doFinal(needEncryptStr.getBytes(UTF_8));
        //对加密结果HEX编码； 解密时也就需要使用HEX解码；
        byte[] encode = Hex.encode(encrypted);
        // 使用BASE64做转码
        // return new BASE64Encoder().encode(encode);
        return new String(encode).toUpperCase();
    }

    /**
     * 解密
     *
     * @param needDecryptStr 秘钥
     * @param key            秘钥
     * @param needDecryptStr
     * @return
     * @throws Exception
     */
    public static String decrypt(String needDecryptStr, String key) throws Exception {
        byte[] raw = key.getBytes(UTF_8);
        //设置秘钥
        SecretKeySpec keySpec = new SecretKeySpec(raw, AES);
        //设置向量
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        //初始化解密方式  Cipher.DECRYPT_MODE 解密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        //获取HEX的解码，因为在是加密过程中采用了HEX的编码，所以此步骤需要HEX的解码
        //如果在是加密过程中采用了base64的编码，此步骤就需要base64的解码，
        //HEX和base64 使用一种即可，但需要保持一致
        byte[] encrypted1 = Hex.decode(needDecryptStr);
        // 先用base64解密。与上一行HEX两者选其一。
        // byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);

        //解密
        byte[] original = cipher.doFinal(encrypted1);
        return new String(original, UTF_8);
    }
}
