package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/7/26 15:15
 */
@Data
public class VendorManagementListDataVO extends AdviceObject {

    @ApiModelProperty(value =  "id")
    private Long id;

    @ApiModelProperty(value =  "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value =  "统一社会信用代码")
    private String socialCreditCode;

    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value =  "注册资金")
    private BigDecimal registeredCapital;

    @ApiModelProperty(value =  "企业所在地")
    private String vendorLocation;

    @ApiModelProperty(value =  "企业分类")
    private String enterpriseType;

    @ApiModelProperty(value =  "企业分类-文本")
    private String enterpriseTypeText;

}
