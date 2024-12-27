package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 合同签约方信息对象 tb_agreement_party_info
 *
 * @author zs
 * @date 2024-12-27
 */
@Data
@TableName(value = "tb_agreement_party_info")
@EqualsAndHashCode(callSuper = true)
public class AgreementPartyInfo extends BaseEntity{
    private static final long serialVersionUID = 1L;

    /** 合同id */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /** 合同角色类型(数据字典CON_ROLE_TYPE) */
    @ApiModelProperty(value = "合同角色类型(数据字典CON_ROLE_TYPE)")
    private String roleType;

    /** 签约单位编号 */
    @ApiModelProperty(value = "签约单位编号")
    private String signerCode;

    /** 签约单位名称 */
    @ApiModelProperty(value = "签约单位名称")
    private String signerName;

    /** 签约单位所占比例 */
    @ApiModelProperty(value = "签约单位所占比例")
    private BigDecimal signerRate;

    /** 签约单位银行账户名称 */
    @ApiModelProperty(value = "签约单位银行账户名称")
    private String signerBankName;

    /** 签约单位开户支行 */
    @ApiModelProperty(value = "签约单位开户支行")
    private String signerBankOpen;

    /** 签约单位银行账号 */
    @ApiModelProperty(value = "签约单位银行账号")
    private String signerBankAccount;

    /** 签约单位纳税人识别号 */
    @ApiModelProperty(value = "签约单位纳税人识别号")
    private String signerTaxpayerNumber;


}
