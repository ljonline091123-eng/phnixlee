package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/26 15:12
 */
@Data
public class VendorManagementListQueryDataVO {

    @ApiModelProperty(value =  "供应商名称")
    private String enterpriseName;

    @ApiModelProperty(value =  "联系人")
    private String contactName;

    @ApiModelProperty(value =  "联系电话")
    private String contactPhone;

    @ApiModelProperty(value =  "企业分类")
    private String enterpriseType;

    @ApiModelProperty(value =  "供应商分类")
    private Integer vendorClass;

    @ApiModelProperty(value = "企业所在省/市编码")
    private String enterpriseProvinceCode;

    @ApiModelProperty(value = "企业所在地市编码")
    private String enterpriseCityCode;

    @ApiModelProperty(value = "企业分类集合")
    private List<Long> classifyIds;

    @ApiModelProperty(value = "注册资金")
    private BigDecimal registeredCapital;

    @ApiModelProperty(value = "供应商等级")
    private Integer vendorLevel;

}
