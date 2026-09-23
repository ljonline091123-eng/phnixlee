package com.zhaocai.common.signature.controller;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateCallBackTypeEnum;
import com.zhaocai.common.signature.dto.callback.qysp.QiYueSuoPrivateCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.CompanyAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.PersonAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;
import com.zhaocai.common.signature.service.impl.QiYueSuoPrivateCallBackService;
import com.zhaocai.common.signature.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;


/**
 * 契约锁回调
 *
 * @author chenming
 * @date 2024-09-10
 */
@Slf4j
@RestController
@RequestMapping("/qiYueSuo")
public class QiYueSuoPrivateCallBackController {

    @Value("${signature.qiyuesuo.callback.aesKey}")
    private String callBackAesKey;

    @Value("${signature.qiyuesuo.callback.token}")
    private String callBackToken;

    @Autowired
    private QiYueSuoPrivateCallBackService qiYueSuoPrivateCallBackService;

    /**
     * 个人认证回调接口
     */
    @PostMapping("/personAuthCallBack")
    public String personAuthCallBack(@RequestBody QiYueSuoPrivateCallBackData callBackData)  {
        try {
            PersonAuthCallBackData personAuthCallBack = getCallBackDTO(callBackData.getEncrypted(), PersonAuthCallBackData.class);
            if (QiYueSuoPrivateCallBackTypeEnum.isCallbackCheck(personAuthCallBack.getCallbackType())) {
                log.warn("[契约锁回调] - 这是个人认证回调验证，不做处理....");
                return "success";
            }

            qiYueSuoPrivateCallBackService.personAuthCallBack(personAuthCallBack);
        } catch (Exception e) {
            log.error("[契约锁回调] - 处理个人授权认证回调失败，cause by:{}",e.getMessage(),e);
            return "fail|" + e.getMessage();
        }
        return "success";
    }

    /**
     * 企业认证回调接口
     */
    @PostMapping("/companyAuthCallBack")
    public String companyAuthCallBack(@RequestBody QiYueSuoPrivateCallBackData callBackData) {
        try {
            CompanyAuthCallBackData companyAuthCallBackData = getCallBackDTO(callBackData.getEncrypted(), CompanyAuthCallBackData.class);
            if (QiYueSuoPrivateCallBackTypeEnum.isCallbackCheck(companyAuthCallBackData.getCallbackType())) {
                log.warn("[契约锁回调] - 这是企业认证回调验证，不做处理....");
                return "success";
            }
            qiYueSuoPrivateCallBackService.companyAuthCallBack(companyAuthCallBackData);
        } catch (Exception e) {
            log.error("[契约锁回调] - 处理企业授权认证回调失败，cause by:{}",e.getMessage(),e);
            return "fail|" + e.getMessage();
        }

        return "success";
    }

    /**
     * 电子签章回调
     */
    @PostMapping("/signCallBack")
    public String signCallBack(@RequestBody QiYueSuoPrivateCallBackData callBackData) {
        try {
            SignCallBackData signCallBackData = getCallBackDTO(callBackData.getEncrypted(), SignCallBackData.class);
            if (QiYueSuoPrivateCallBackTypeEnum.isCallbackCheck(signCallBackData.getCallbackType())) {
                log.warn("[契约锁回调] - 这是电子签章回调验证，不做处理....");
                return "success";
            }
            qiYueSuoPrivateCallBackService.signCallBack(signCallBackData);
        } catch (Exception e) {
            log.error("[契约锁回调] - 处理电子签章回调失败，cause by:{}",e.getMessage(),e);
            return "fail|" + e.getMessage();
        }

        return "success";
    }

    /**
     * 获取回调 DTO
     * @param content
     * @return
     */
    private <T> T getCallBackDTO(String content,Class<T> callBackClass) throws Exception{
        log.info("[契约锁回调] - 收到契约锁回调，content:{}",content);
        String decryptContent = decrypt(content,callBackAesKey,callBackToken);
        log.info("[契约锁回调] - 契约锁回调内容解密成功，decryptContent:{}",decryptContent);
        return JacksonUtil.toObject(decryptContent,callBackClass);
    }

    /**
     * 解密回调密文
     * @param encryptedText
     * @param aesKey
     * @param token
     * @return
     * @throws Exception
     */
    private String decrypt(String encryptedText, String aesKey, String token) throws Exception {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] ivBytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes, 0, 16);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
