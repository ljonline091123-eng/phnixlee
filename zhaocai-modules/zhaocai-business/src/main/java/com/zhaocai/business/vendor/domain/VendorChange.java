package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 供应商变更对象 tb_vendor_change
 *
 * @author lsn
 * @date 2024-08-05
 */
@Getter
@Setter
@TableName(value = "tb_vendor_change")
public class VendorChange extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Long vendorId;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private Integer version;

    /**
     * 变更状态
     */
    @ApiModelProperty(value = "变更状态")
    private Integer changeStatus;

    /**
     * 企业名称
     */
    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    /**
     * 企业编码
     */
    @ApiModelProperty(value = "企业编码")
    private String enterpriseCode;

    /**
     * 是否外部客商
     */
    @ApiModelProperty(value = "是否外部客商")
    private Integer isExternal;

    /**
     * 客商属性
     */
    @ApiModelProperty(value = "客商属性")
    private Integer vendorAttribute;

    /**
     * 成立时间
     */
    @ApiModelProperty(value = "成立时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date establishDate;

    /**
     * 客商状态
     */
    @ApiModelProperty(value = "客商状态")
    private Integer vendorStatus;

    /**
     * 创建单位
     */
    @ApiModelProperty(value = "创建单位")
    private String createdUnit;

    /**
     * 统一社会信用代码
     */
    @ApiModelProperty(value = "统一社会信用代码")
    private String socialCreditCode;

    /**
     * 注册资金
     */
    @ApiModelProperty(value = "注册资金")
    private BigDecimal registeredCapital;

    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private Integer currencyCode;

    /**
     * 开户支行
     */
    @ApiModelProperty(value = "开户支行")
    private String accountBranch;

    /**
     * 银行账号
     */
    @ApiModelProperty(value = "银行账号")
    private String bankAccount;

    /**
     * 法定代表人
     */
    @ApiModelProperty(value = "法定代表人")
    private String legalRepresentative;

    /**
     * 法人联系方式
     */
    @ApiModelProperty(value = "法人联系方式")
    private String legalPhone;

    /**
     * 法人身份证号码
     */
    @ApiModelProperty(value = "法人身份证号码")
    private String legalIdCard;

    /**
     * 企业分类
     */
    @ApiModelProperty(value = "企业分类")
    private String enterpriseType;

    /**
     * 增值税纳税人类型
     */
    @ApiModelProperty(value = "增值税纳税人类型")
    private Integer taxpayerType;

    /**
     * 发票类型
     */
    @ApiModelProperty(value = "发票类型")
    private Integer invoiceType;

    /**
     * 企业性质
     */
    @ApiModelProperty(value = "企业性质")
    private Integer enterpriseNature;

    /**
     * 企业所在省/市编码
     */
    @ApiModelProperty(value = "企业所在省/市编码")
    private String enterpriseProvinceCode;

    /**
     * 企业所在省/市名称
     */
    @ApiModelProperty(value = "企业所在省/市名称")
    private String enterpriseProvinceName;

    /**
     * 企业所在地市编码
     */
    @ApiModelProperty(value = "企业所在地市编码")
    private String enterpriseCityCode;

    /**
     * 企业所在地市名称
     */
    @ApiModelProperty(value = "企业所在地市名称")
    private String enterpriseCityName;

    /**
     * 详细地址
     */
    @ApiModelProperty(value = "详细地址")
    private String enterpriseAddress;

    /**
     * 企业联系电话
     */
    @ApiModelProperty(value = "企业联系电话")
    private String contactPhone;

    /**
     * 经营范围
     */
    @ApiModelProperty(value = "经营范围")
    private String businessScope;

    /**
     * 企业简介
     */
    @ApiModelProperty(value = "企业简介")
    private String enterpriseProfile;

    /**
     * 首次合作单位
     */
    @ApiModelProperty(value = "首次合作单位")
    private String firstCooperationCompanyCode;

    /**
     * 首次合作单位名称
     */
    @ApiModelProperty(value = "首次合作单位名称")
    private String firstCooperationCompanyName;

    /**
     * 供应商类别
     */
    @ApiModelProperty(value = "供应商类别")
    private Integer vendorClass;

    /**
     * 供应商等级
     */
    @ApiModelProperty(value = "供应商等级")
    private Integer vendorLevel;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer state;

    /**
     * 是否为黑名单
     */
    @ApiModelProperty(value = "是否为黑名单")
    private Integer isBlack;

    /**
     * 黑名单限制期-开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-开始日期")
    private Date blackBeginDate;

    /**
     * 黑名单限制期-结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-结束日期")
    private Date blackEndDate;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;

    /**
     * 批语
     */
    @TableField(exist = false)
    private String operateComment;
}
