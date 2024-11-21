package com.zhaocai.business.vendor.vo.res;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 供应商vo
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorVO extends AdviceObject {

    @ApiModelProperty(value = "供应商 id")
    private Long id;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value =  "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value =  "统一社会信用代码")
    private String socialCreditCode;

    @ApiModelProperty(value =  "注册资金")
    private BigDecimal registeredCapital;

    @ApiModelProperty(hidden = true)
    private Integer currencyCode;

    @ApiModelProperty(value =  "开户支行")
    private String accountBranch;

    @ApiModelProperty(value =  "银行账号")
    private String bankAccount;

    @ApiModelProperty(value =  "法定代表人")
    private String legalRepresentative;

    @ApiModelProperty(value = "法人联系方式")
    private String legalPhone;

    @ApiModelProperty(value = "法人身份证号码")
    private String legalIdCard;

    @ApiModelProperty(hidden = true)
    private String enterpriseType;

    @ApiModelProperty(hidden = true)
    private Integer taxpayerType;

    @ApiModelProperty(hidden = true)
    private Integer invoiceType;

    @ApiModelProperty(hidden = true)
    private Integer enterpriseNature;

    @ApiModelProperty(value =  "企业所在省/市名称")
    private String enterpriseProvinceName;

    @ApiModelProperty(value =  "企业所在地市名称")
    private String enterpriseCityName;

    @ApiModelProperty(value =  "详细地址")
    private String enterpriseAddress;

    @ApiModelProperty(value =  "企业联系电话")
    private String contactPhone;

    @ApiModelProperty(value =  "经营范围")
    private String businessScope;

    @ApiModelProperty(value =  "企业简介")
    private String enterpriseProfile;

    @ApiModelProperty(value = "首次合作单位名称")
    private String firstCooperationCompanyName;

    @ApiModelProperty(value = "首次合作单位联系人名称")
    private String firstCooperationContactName;

    @ApiModelProperty(value = "首次合作单位联系号码")
    private String firstCooperationContactPhone;

    @ApiModelProperty(value =  "供应商类别")
    private Integer vendorClass;

    @ApiModelProperty(value =  "供应商等级")
    private Integer vendorLevel;

    @DictCache(dictBizEnum= DictBizEnum.CURRENCY,filedName = "currencyCode")
    @ApiModelProperty(value =  "币种-文本")
    private String currencyCodeText;

    @ApiModelProperty(value =  "企业分类-文本")
    private String enterpriseTypeText;

    @DictCache(dictBizEnum= DictBizEnum.TAXPAYER_TYPE,filedName = "taxpayerType")
    @ApiModelProperty(value =  "增值税纳税人类型-文本")
    private String taxpayerTypeText;

    @DictCache(dictBizEnum= DictBizEnum.INVOICE_TYPE,filedName = "invoiceType")
    @ApiModelProperty(value =  "发票类型-文本")
    private String invoiceTypeText;

    @DictCache(dictBizEnum= DictBizEnum.ENTERPRISE_NATURE,filedName = "enterpriseNature")
    @ApiModelProperty(value =  "企业性质-文本")
    private String enterpriseNatureText;

    @DictCache(dictBizEnum= DictBizEnum.VENDOR_CLASS,filedName = "vendorClass")
    @ApiModelProperty(value =  "供应商类别-文本")
    private String vendorClassText;

    @DictCache(dictBizEnum= DictBizEnum.VENDOR_LEVEL,filedName = "vendorLevel")
    @ApiModelProperty(value =  "供应商等级-文本")
    private String vendorLevelText;

    @ApiModelProperty(value =  "是否为黑名单")
    private Integer isBlack;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-开始日期")
    private Date blackBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-结束日期")
    private Date blackEndDate;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;

    @MoneyFormat(filedName = "registeredCapital",scale = 2)
    @ApiModelProperty(value =  "注册资金（千分位）")
    private String registeredCapitalPattern;

    @ApiModelProperty(value = "流程类型")
    private Integer processType;

    @ApiModelProperty(value = "审批状态")
    private Integer state;



    /** 变更id */
    @TableField(exist = false)
    private Long changeId;

    /**
     * 批语
     */
    @ApiModelProperty(value = "批语")
    private String operateComment;
}
