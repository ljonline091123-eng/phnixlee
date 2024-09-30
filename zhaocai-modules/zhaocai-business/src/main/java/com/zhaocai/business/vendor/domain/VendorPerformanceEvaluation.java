package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商履约评价对象 tb_vendor_performance_evaluation
 *
 * @author WH
 * @date 2024-07-12
 */
@Data
@TableName(value = "tb_vendor_performance_evaluation")
public class VendorPerformanceEvaluation extends BaseEntity {

    /**
     * 项目id
     */
    @ApiModelProperty(value = "项目id")
    private String projectId;

    /**
     * 供应商 id
     */
    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    /**
     * 合同编号
     */
    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    /**
     * 合同名称
     */
    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    /**
     * 甲方id
     */
    @ApiModelProperty(value = "甲方id")
    private String partyAId;

    /**
     * 甲方名称
     */
    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    /**
     * 乙方id
     */
    @ApiModelProperty(value = "乙方id")
    private String partyBId;

    /**
     * 乙方名称
     */
    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    /**
     * 优-数量
     */
    @ApiModelProperty(value = "优-数量")
    private BigDecimal excellentNum;

    /**
     * 良-数量
     */
    @ApiModelProperty(value = "良-数量")
    private BigDecimal goodNum;

    /**
     * 合格-数量
     */
    @ApiModelProperty(value = "合格-数量")
    private BigDecimal qualifiedNum;

    /**
     * 差-数量
     */
    @ApiModelProperty(value = "差-数量")
    private BigDecimal badNum;
}
