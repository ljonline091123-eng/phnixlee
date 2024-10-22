package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 投标单信息对象 tb_bidding_info
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_info")
public class BiddingInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 父主键id */
    @ApiModelProperty(value =  "父主键id")
    private Long parentId;

    /** 招标项目id */
    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 含税总价 */
    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    /** 不含税总价 */
    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    /** 联系人 */
    @ApiModelProperty(value =  "联系人")
    private String contact;

    /** 联系电话 */
    @ApiModelProperty(value =  "联系电话")
    private String phone;

    /** 提交状态（0草稿 1已提交） */
    @ApiModelProperty(value =  "提交状态（0草稿 1已提交）")
    private Integer submitStatus;

    /** 投标状态（0未投标/ 1已投标（对供应商端）|已回标（对采购端）/ 2已撤回 / 3已废标） */
    @ApiModelProperty(value =  "投标状态")
    private Integer biddingStatus;

    /** 操作人ip */
    @ApiModelProperty(value =  "操作人ip")
    private String ipAddress;

    /** 供应商id */
    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    /** 供应商名称 */
    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    /** 是否收取保证金（为2则不需收取 0未收取 1已收取） */
    @ApiModelProperty(value =  "是否收取保证金（为2则不需收取 0未收取 1已收取）")
    private Integer collectDeposit;

    /** 二次报价设置（默认0关 1开） */
    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    /** 二次报价截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    /* 每开启一次二次报价，就将选中范围的供应商报价 复制一份并升级版本将twiceQuot状态打开。前端通过招标对象的版本和当前版本对比和二次报价开关对比进行开放是否 供应商可以报价 */
    /** 二次报价版本号，对应招标对象的版本号，如果对应不上就是在第*次开启报价时未选中或者是供应商未调价 管理端控制发版号 */
    @ApiModelProperty(value =  "二次报价版本号。从1开始")
    private Integer twiceQuotVersion;

    /** 供应商调价状态(当前二次报价版本) 未被选中进行二次报价的供应商状态为 0未调价 选中的供应商报价了 状态为 1已调价 选中的未进行报价的供应商状态为 2放弃调价  */
    @ApiModelProperty(value =  "供应商调价状态")
    private Integer priceChangeState;

    /** 0未评标 1已评标 */
    @ApiModelProperty(value =  "专家评标状态")
    private Integer expertState;
}
