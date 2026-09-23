package com.zhaocai.common.signature.common.enums;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.sign.qysp.CreateByCategoryResponse;
import com.zhaocai.common.signature.dto.sign.qysp.QiYueSuoResponse;
import lombok.AllArgsConstructor;
import net.qiyuesuo.v3sdk.model.auth.response.UserauthAuthurl2Response;
import net.qiyuesuo.v3sdk.model.company.response.CompanyauthH5pageResponse;
import net.qiyuesuo.v3sdk.model.contract.response.ContractSignurlV3Response;
import net.qiyuesuo.v3sdk.model.v2document.response.V2DocumentCreatebyfileResponse;

/**
 * 契约锁请求
 * @author chenming
 * @date 2024-08-20
 */
@AllArgsConstructor
public enum QiYueSuoPrivateRequestTypeEnum implements RequestType{
    COMPANY_AUTH("company_auth","公司认证","/companyauth/pcpage", CompanyauthH5pageResponse.class),
    PERSON_AUTH("person_auth","个人认证","/userauth/authurl2", UserauthAuthurl2Response.class),
    CREATE_BY_FILE("create_by_file","创建签署文档","/v2/document/createbyfile", V2DocumentCreatebyfileResponse.class),
    CREATE_BY_CATEGORY ("create_by_category","创建电子签约","/v2/document/createbyfile", CreateByCategoryResponse.class),
    GET_SIGN_URL ("get_sign_url","获取签署链接","/contract/signurl/v3", ContractSignurlV3Response.class),
    DOWNLOAD_DOCUMENT ("download_document","下载电子签章文档","/document/download", Object.class),
    CANCEL_CONTRACT ("cancel_contract","作废合同","/contract/cancel", QiYueSuoResponse.class),

    ;

    /**
     * 请求编码
     */
    private final String requestCode;

    /**
     * 请求
     */
    private final String requestName;

    /**
     * 请求 url
     */
    private final String requestUrl;

    /**
     * 结果对象
     */
    private final Class<?> resultClass;

    @Override
    public String getRequestCode() {
        return this.requestCode;
    }

    @Override
    public String getRequestName() {
        return this.requestName;
    }

    @Override
    public String getRequestUrl() {
        return this.requestUrl;
    }

    @Override
    public Class<?> getResultClass() {
        return this.resultClass;
    }

    @Override
    public String getPlatform() {
        return "契约锁";
    }
}
