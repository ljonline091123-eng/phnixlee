package com.zhaocai.common.signature.common.enums;

import com.qiyuesuo.sdk.v2.bean.Contract;
import com.qiyuesuo.sdk.v2.response.ContractPageResult;
import com.qiyuesuo.sdk.v2.response.DocumentAddResult;
import lombok.AllArgsConstructor;

/**
 * 契约锁请求
 * @author chenming
 * @date 2024-08-20
 */
@AllArgsConstructor
public enum QiYueSuoRequestTypeEnum implements RequestType{
    CONTRACT_DRAFT("contract_draft","创建合同草稿","/v2/contract/draft", Contract.class),
    ADD_BY_FILE("add_file","用文件添加合同文档","/v2/document/addbyfile", DocumentAddResult.class),
    CONTRACT_SEND("contract_send","发起合同","/v2/contract/send", Object.class),
    CONTRACT_PAGE_URL("contract_page_url","签署页面","/v2/contract/pageurl", ContractPageResult.class),
    DOCUMENT_DOWNLOAD("document_download","下载合同文档","/v2/document/download", Object.class),

    ;

    /**
     * 请求编码
     */
    private final String businessCode;

    /**
     * 请求
     */
    private final String businessName;

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
        return this.businessCode;
    }

    @Override
    public String getRequestName() {
        return this.businessName;
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
