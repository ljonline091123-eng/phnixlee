package com.zhaocai.business.receipt.domain;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 供应商评价统计对象 tb_receipt_contract_settle_evaluate
 *
 * @author cff
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settle_evaluate")
@ApiModel(value = "ReceiptContractSettleEvaluate对象", description = "供应商评价统计对象")
public class ReceiptContractSettleEvaluate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 差
     */
    @ApiModelProperty("差")
    private Integer badNum;

    /**
     * 优
     */
    @ApiModelProperty("优")
    private Integer excellentNum;

    /**
     * 良
     */
    @ApiModelProperty("良")
    private Integer goodNum;

    /**
     * 合格
     */
    @ApiModelProperty("合格")
    private Integer qualifiedNum;

    /**
     * 评价维度
     */
    @ApiModelProperty("评价维度")
    private String type;

    /**
     * 父级id
     */
    @ApiModelProperty("父级id")
    private String parntId;

    /**
     *
     */
    @ApiModelProperty("")
    private String createDept;

    /**
     * 创建人 id
     */
    @ApiModelProperty("创建人 id")
    private Long createId;

    /**
     * 修改人 id
     */
    @ApiModelProperty("修改人 id")
    private Long updateId;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @ApiModelProperty("删除标志（0代表存在 2代表删除）")
    private String delFlag;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("thirdId", getThirdId())
                .append("badNum", getBadNum())
                .append("excellentNum", getExcellentNum())
                .append("goodNum", getGoodNum())
                .append("qualifiedNum", getQualifiedNum())
                .append("type", getType())
                .append("parntId", getParntId())
                .append("createDept", getCreateDept())
                .append("createBy", getCreateBy())
                .append("createId", getCreateId())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateId", getUpdateId())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .toString();
    }
}
