package com.zhaocai.business.vendor.vo.req;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 供应商评价对象 tb_vendor_evaluate
 *
 * @author xw
 * @date 2025-10-11
 */
@Data
@ApiModel(value = "列表查询参数")
public class TbVendorEvaluateQueryVo extends PageRecive {

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

    private String evaluateTime;

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

    private String createBy;

    private Date createTime;

    private String updateBy;

    private String updateTime;

}
