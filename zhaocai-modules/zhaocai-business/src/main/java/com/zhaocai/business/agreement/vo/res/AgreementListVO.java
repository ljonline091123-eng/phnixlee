package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同列表
 *
 * @author chenming
 * @date 2024/06/05
 */
@Data
public class AgreementListVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "合同总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "填报人")
    private String reporterName;

    @ApiModelProperty(hidden = true)
    private Integer agreementState;

    @ApiModelProperty(value = "是否可操作")
    private Integer isOperate = 1;

    @ApiModelProperty(value = "支出业务分类")
    private Integer expenditureBusinessType;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_STATE,filedName = "agreementState")
    @ApiModelProperty(value = "合同状态-文本")
    private String agreementStateText;

    @MoneyFormat(filedName = "totalAmount",scale = 2)
    private String totalAmountText;

    @ApiModelProperty(hidden = true)
    private String wfProcessId;

    @ApiModelProperty(value = "甲方所属机构 id")
    private Long partyADeptId;

    @ApiModelProperty(value = "创建用户")
    private Long createById;

    @ApiModelProperty(value = "签章用户")
    private Long signatureUserId;

    @ApiModelProperty(value = "采购方案招标编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式-文本")
    private String procurementTypeText;

    @ApiModelProperty(value = "招标id")
    private Long noticeId;

    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;
}
