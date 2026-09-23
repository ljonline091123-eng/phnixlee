package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 供应商管理列表v
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
public class VendorManagementListVO extends AdviceObject {

    @ApiModelProperty(value =  "id")
    private Long id;

    @ApiModelProperty(value =  "enterpriseCode")
    private String enterpriseCode;

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

    @JsonIgnore
    @ApiModelProperty(value =  "企业分类")
    private String enterpriseType;

    @ApiModelProperty(value =  "企业分类-文本")
    private String enterpriseTypeText;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value =  "创建时间")
    private String createTime;

    @ApiModelProperty(value =  "供应商库类型")
    private String vendorLibraryText;

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Integer vendorClass;

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Integer isBlack;

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Integer state;

    @ApiModelProperty(value = "供应商合作金额")
    private BigDecimal cooperationAmount;

    @ApiModelProperty(value = "供应商评价(优)")
    private Long excellentNum;

    @MoneyFormat(filedName = "registeredCapital")
    @ApiModelProperty(value =  "注册资金")
    private String registeredCapitalText;

    @MoneyFormat(filedName = "cooperationAmount")
    @ApiModelProperty(value = "供应商合作金额")
    private String cooperationAmountText;

    @ApiModelProperty(value = "供应商合作记录")
    private Long cooperationNum;

    @ApiModelProperty(value = "流程类型")
    private Integer processType;

    @ApiModelProperty(value = "首次合作单位")
    private String firstCooperationCompanyCode;

    @ApiModelProperty(value = "首次合作单位名称")
    private String firstCooperationCompanyName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value =  "注册时间")
    private String registerApprovalTime;

    @ApiModelProperty(hidden = true)
    private String vendorState;

    @ApiModelProperty(hidden = true)
    private String vendorStateTwo;

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

    @ApiModelProperty(value =  "是否外部客商")
    private String isExternal;

    @ApiModelProperty(hidden = true)
    private String enableStatus;

/*    @ApiModelProperty(value =  "企业编号")
    private String enterpriseCode;*/

    public Long getExcellentNum() {
        return excellentNum == null ? 0 : excellentNum;
    }

    public Long getCooperationNum() {
        return cooperationNum == null ? 0 : cooperationNum;
    }
}
