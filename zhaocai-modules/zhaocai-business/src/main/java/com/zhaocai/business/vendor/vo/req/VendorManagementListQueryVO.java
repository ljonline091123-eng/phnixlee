package com.zhaocai.business.vendor.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商管理列表查询
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
public class VendorManagementListQueryVO extends PageRecive {

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

    @ApiModelProperty(value = "流程类型")
    private Integer processType;

    @ApiModelProperty(value =  "银行账号")
    private String bankAccount;

    @ApiModelProperty(value =  "统一社会信用代码")
    private String socialCreditCode;

    @ApiModelProperty(value = "首次合作单位")
    private String firstCooperationCompanyCode;

    @ApiModelProperty(value =  "企业所在地")
    private String vendorLocation;

    @ApiModelProperty(hidden = true)
    private String vendorState;

    @ApiModelProperty(hidden = true)
    private String regionCityCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(hidden = true)
    private String startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(hidden = true)
    private String endDate;

    @ApiModelProperty(hidden = true)
    private BigDecimal registeredCapitalStart;

    @ApiModelProperty(hidden = true)
    private BigDecimal registeredCapitalEnd;


}
