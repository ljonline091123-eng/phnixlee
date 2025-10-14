package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.simpleframework.xml.Transient;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 供应商评价对象 tb_vendor_evaluate
 *
 * @author xw
 * @date 2025-10-11
 */
@Getter
@Setter
@TableName(value = "tb_vendor_evaluate")
public class TbVendorEvaluate extends AdviceObject {

    private static final long serialVersionUID = 1L;

    /** id */
    private Long id;

    /** 评价编号 */
    private String evaluateCode;

    /** 供应商id */
    private Long vendorId;

    /** 供应商名称 */
    private String vendorName;

    /** 评分类型 */
    private String evaluateType;

    /** 评分周期 */
    private Date evaluateTime;


    /** 评分 */
    private BigDecimal evaluateFraction;

    /** 创建人 id */
    private Long createId;

    /** 修改人 id */
    private Long updateId;

    /** 删除标志（0代表存在 2代表删除） */
    @TableLogic
    private String delFlag;

    /** 评价状态 */
    private String evaluateStatus;

    /** 是否合格 */
    private String isQualified;

    /** 评分周期文字 */
    private String evaluateTimeTxt;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;


    @DictCache(dictBizEnum= DictBizEnum.evaluate_type,filedName = "evaluateType")
    @TableField(exist = false)
    private String evaluateTypeName;

    @DictCache(dictBizEnum= DictBizEnum.evaluate_status,filedName = "evaluateStatus")
    @TableField(exist = false)
    private String evaluateStatusName;

    @DictCache(dictBizEnum= DictBizEnum.sys_yes_no,filedName = "isQualified")
    @TableField(exist = false)
    private String isQualifiedName;

    @DictCache(dictBizEnum= DictBizEnum.evaluate_time,filedName = "evaluateTimeTxt")
    @TableField(exist = false)
    private String evaluateTimeTxtName;







}
