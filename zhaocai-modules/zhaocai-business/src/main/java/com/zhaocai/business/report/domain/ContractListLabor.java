package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同-劳务合同清单 contract_list_labor
 *
 * @author lsn
 * @date 2024-10-13
 */
@Data
@TableName(value = "contract_list_labor")
public class ContractListLabor {

  @ApiModelProperty(value = "主键")
  private String id;

  @ApiModelProperty(value = "唯一id")
  private String uniqueId;

  @ApiModelProperty(value = "合同ID")
  private String conId;

  @ApiModelProperty(value = "成本子目编码")
  private String subjectDtlCode;

  @ApiModelProperty(value = "成本子目名称")
  private String subjectDtlName;

  @ApiModelProperty(value = "特征描述")
  private String specs;

  @ApiModelProperty(value = "计量单位，如m³")
  private String measureUnit;

  @ApiModelProperty(value = "计量规则，如按体积计算")
  private String metrologicalRules;

  @ApiModelProperty(value = "工作内容")
  private String basicJob;

  @ApiModelProperty(value = "成本科目档案ID,关联xxx表")
  private String subjectId;

  @ApiModelProperty(value = "成本科目编码")
  private String subjectCode;

  @ApiModelProperty(value = "成本科目名称")
  private String subjectName;

  @ApiModelProperty(value = "可使用工程量（快照）")
  private BigDecimal surplusQuantity;

  @ApiModelProperty(value = "工程量")
  private BigDecimal quantity;

  @ApiModelProperty(value = "税率")
  private BigDecimal taxRate;

  @ApiModelProperty(value = "单价（不含税）")
  private BigDecimal ntaxPrice;

  @ApiModelProperty(value = "单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）")
  private BigDecimal taxPrice;

  @ApiModelProperty(value = "金额（不含税）")
  private BigDecimal ntaxAmount;

  @ApiModelProperty(value = "金额（含税）")
  private BigDecimal taxAmount;

  @ApiModelProperty(value = "税额，单价（不含税）*税率，自动算出来的不可以更改")
  private BigDecimal tax;

  @ApiModelProperty(value = "结算状态（0-未结算，1-部分结算，2-已结算）")
  private String settleStatus;

  @ApiModelProperty(value = "删除状态（0-生效 其他删除）")
  private Integer valid;

}
