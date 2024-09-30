package com.zhaocai.common.signature.dto.command;


import lombok.Getter;
import lombok.Setter;

/**
 * 公司认证命令请求参数
 *
 * @author chenming
 * @date 2024-09-09
 */
@Getter
@Setter
public class CompanyAuthCommandRequest extends SignatureCommandRequest{

    protected CompanyAuthCommandRequest() {
    }

    /**
     * 公司/组织机构 id
     */
    private Long companyId;

    /**
     * 公司/组织机构名称
     */
    private String companyName;

    /**
     * 统一社会信用代码
     */
    private String socialCreditCode;

    /**
     * 法人名称
     */
    private String legalPersonName;

    /**
     * 法人联系方式
     */
    private String legalPersonPhone;
}
