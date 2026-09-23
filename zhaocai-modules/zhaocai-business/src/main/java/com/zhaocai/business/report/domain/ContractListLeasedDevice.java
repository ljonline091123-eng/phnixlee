package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同-租赁设备合同清单 contract_list_leased_device
 *
 * @author lsn
 * @date 2024-10-13
 */
@Data
@TableName(value = "contract_list_leased_device")
public class ContractListLeasedDevice {

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

  @ApiModelProperty(value = "规格型号（特征描述）")
  private String specs;

  @ApiModelProperty(value = "计量单位，如m³")
  private String measureUnit;

  @ApiModelProperty(value = "品牌")
  private String brand;

  @ApiModelProperty(value = "租赁方式(1-日 2-月租 3-工作量)")
  private String rentMode;

  @ApiModelProperty(value = "计租单位(1-㎡ 2-m³ 3-次)只有租赁方式是工作量时选择")
  private String rentUnit;

  @ApiModelProperty(value = "成本科目档案ID,关联xxx表")
  private String subjectId;

  @ApiModelProperty(value = "成本科目编码")
  private String subjectCode;

  @ApiModelProperty(value = "成本科目名称")
  private String subjectName;

  @ApiModelProperty(value = "可使用数量（快照）")
  private BigDecimal surplusQuantity;

  @ApiModelProperty(value = "数量")
  private BigDecimal number;

  @ApiModelProperty(value = "租赁时间")
  private BigDecimal rentTime;

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

  @ApiModelProperty(value = "删除状态（0-生效 其他删除）")
  private Integer valid;

}
