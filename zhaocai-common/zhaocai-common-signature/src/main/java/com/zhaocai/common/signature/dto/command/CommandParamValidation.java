package com.zhaocai.common.signature.dto.command;

import com.qiyuesuo.sdk.v2.utils.StringUtils;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;

/**
 * 命令参数验证
 *
 * @author chenming
 * @date 2024-09-13
 */
public class CommandParamValidation {

    /**
     * 基础参数校验
     * @param businessCode
     * @param businessId
     * @param applicantName
     * @param applicantMobile
     */
    public static void commonParamsValidate(String businessCode, Long businessId, String applicantName, String applicantMobile) {
        checkParamsNotBlank(businessCode,"业务编码");
        checkParamsNotNullAndZero(businessId,"业务 id ");
        checkParamsNotBlank(applicantName,"申请人名称");
        checkParamsNotBlank(applicantMobile,"申请人电话号码");
        checkParamsNotBlank(businessCode,"业务编码");
    }

    /**
     * 公司授权参数校验
     * @param businessCode
     * @param businessId
     * @param applicantName
     * @param applicantMobile
     * @param companyName
     * @param socialCreditCode
     */
    public static void companyAuthParamsValidate(String businessCode, Long businessId, String applicantName, String applicantMobile,
                                                 String companyName, String socialCreditCode) {
        commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        checkParamsNotBlank(companyName,"公司名称");
        checkParamsNotBlank(socialCreditCode,"公司统一社会信用代码");
    }

    private static void checkParamsNotNullAndZero(Long params,String paramsName) {
        if (params == null || params.equals(0L)) {
            throw new SignatureValidateException(paramsName + "不能为空");
        }
    }

    private static void checkParamsNotBlank(String params,String paramsName) {
        if (StringUtils.isBlank(params)) {
            throw new SignatureValidateException(paramsName + "不能为空");
        }
    }
}
