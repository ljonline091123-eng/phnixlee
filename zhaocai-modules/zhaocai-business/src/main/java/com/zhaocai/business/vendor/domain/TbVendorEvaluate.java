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
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.simpleframework.xml.Transient;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    @Excel(name = "编号")
    private String evaluateCode;

    /** 供应商id */
    private Long vendorId;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String vendorName;

    /** 评分类型 */
    private String evaluateType;

    /** 评分周期 */
    private Date evaluateTime;


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


    @DictCache(dictBizEnum= DictBizEnum.evaluate_status,filedName = "evaluateStatus")
    @TableField(exist = false)
    @Excel(name = "状态")
    private String evaluateStatusName;

    @DictCache(dictBizEnum= DictBizEnum.evaluate_type,filedName = "evaluateType")
    @TableField(exist = false)
    @Excel(name = "评价类型")
    private String evaluateTypeName;

    @TableField(exist = false)
    @Excel(name = "评价周期")
    private String evaluateTimeTxtName;

    /** 评分 */

    @Excel(name = "评价分数")
    private BigDecimal evaluateFraction;

    @DictCache(dictBizEnum= DictBizEnum.sys_yes_no,filedName = "isQualified")
    @TableField(exist = false)
    @Excel(name = "是否合格")
    private String isQualifiedName;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @Excel(name = "评价时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 更新者 */
    @ApiModelProperty(value = "更新者")
    @Excel(name = "评价人")
    private String updateBy;



    @ApiModelProperty(value = "附件")
    @TableField(exist = false)
    private List<AttachmentRequestVO> fileList;







}
