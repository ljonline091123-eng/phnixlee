package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 获取法人单位认证页面
 *
 * @author chenming
 * @date 2024-09-09
 */
@Getter
@Setter
public class CompanyAuthRequest extends SignatureRequest {

    public CompanyAuthRequest(SignatureCommandRequest request) {
        super(request);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.COMPANY_AUTH);
    }

    /**
     * 公司/组织机构 id
     */
    private Long companyId;

    /**
     * 法人单位名称
     */
    @NotBlank(message = "法人单位名称不能为空")
    private String companyName;

    /**
     * 统一社会信用代码/工商注册号
     */
    @NotBlank(message = "统一社会信用代码不能为空")
    private String registerNo;

    /**
     * 法定代表人姓名
     */
    private String legalPersonName;

    /**
     * 申请人姓名
     */
    @NotBlank(message = "申请人姓名不能为空")
    private String chargerName;

    /**
     * 申请人手机号
     */
    private String chargerMobile;
}
