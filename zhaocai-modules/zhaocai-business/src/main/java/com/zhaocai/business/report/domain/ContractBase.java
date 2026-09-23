package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支出合同-合同清单 contract_base
 *
 * @author lsn
 * @date 2024-10-11
 */
@Data
@TableName(value = "contract_base")
public class ContractBase {

  @ApiModelProperty(value = "主键")
  private String id;

  @ApiModelProperty(value = "唯一id")
  private String uniqueId;

  @ApiModelProperty(value = "是否引入")
  private Integer introduceFlag;

  @ApiModelProperty(value = "上一版本id")
  private String preId;

  @ApiModelProperty(value = "版本号")
  private Integer version;

  @ApiModelProperty(value = "变更方式")
  private Integer changeMode;

  @ApiModelProperty(value = "变更原因")
  private String changeReasonRemark;

  @ApiModelProperty(value = "是否最新版")
  private Integer lastVersionFlag;

  @ApiModelProperty(value = "合同编码")
  private String conCode;

  @ApiModelProperty(value = "单位内部合同管理编码")
  private String internalCode;

  @ApiModelProperty(value = "合同名称")
  private String conName;

  @ApiModelProperty(value = "项目Id")
  private String projectId;

  @ApiModelProperty(value = "交易标的物")
  private String transactionSubjectMatter;

  @ApiModelProperty(value = "合同类型(合约规划档案id)")
  private String programArchivesId;

  @ApiModelProperty(value = "合同类型(合约规划档案名称)")
  private String programArchivesName;

  @ApiModelProperty(value = "合约规划，规划金额（含税），快照")
  private BigDecimal programArchivesTaxAmount;

  @ApiModelProperty(value = "合约规划，已发生规划金额（含税）, 快照")
  private BigDecimal programArchivesUsedAmount;

  @ApiModelProperty(value = "合约规划，剩余金额（含税）, 快照")
  private BigDecimal programArchivesSurplusAmount;

  @ApiModelProperty(value = "合同甲方,可配置")
  private String partaId;

  @ApiModelProperty(value = "合同甲方名称")
  private String partaName;

  @ApiModelProperty(value = "合同乙方,可配置")
  private String partbId;

  @ApiModelProperty(value = "合同乙方名称")
  private String partbName;

  @ApiModelProperty(value = "乙方法人代表")
  private String partbLegalPerson;

  @ApiModelProperty(value = "乙方法人代表身份证")
  private String partbLegalPersonIdCard;

  @ApiModelProperty(value = "乙方法人代表电话")
  private String partbLegalPersonPhone;

  @ApiModelProperty(value = "乙方现场实际履职负责人")
  private String partbSiteManager;

  @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
  private String partbSiteManagerIdCard;

  @ApiModelProperty(value = "乙方现场实际履职负责人电话")
  private String partbSiteManagerPhone;

  @ApiModelProperty(value = "支出业务分类( Z-其他 A-劳务分包 B-专业分包 C-购买材料 D-租赁材料 G-设备租赁（机械）)")
  private String conType;

  @ApiModelProperty(value = "计租方式 1算头又算尾  2算头不算尾  3算尾不算头  4头尾都不算")
  private String rentType;

  @ApiModelProperty(value = "支付周期（1-按月支付 2-按进度节点支付 3-按节日节点支付）")
  private String paymentCycle;

  @ApiModelProperty(value = "支付方式（1-银行转账 2-商业汇票 3-供应链金融产品  4-固定资产抵扣）")
  private String paymentType;

  @ApiModelProperty(value = "进场日期（年月日）")
  private Date startDate;

  @ApiModelProperty(value = "完工日期（年月日）")
  private Date endDate;

  @ApiModelProperty(value = "工期")
  private BigDecimal duration;

  @ApiModelProperty(value = "合同状态")
  private String conStatus;

  @ApiModelProperty(value = "结算状态")
  private String settleStatus;

  @ApiModelProperty(value = "合同签订日期（年月日）")
  private Date signDate;

  @ApiModelProperty(value = "合同生效日期（年月日）")
  private Date validDate;

  @ApiModelProperty(value = "价格形式(1-固定单价2-总价下浮3-固定总价)")
  private String priceForm;

  @ApiModelProperty(value = "下浮比例(%)")
  private BigDecimal downRatio;

  @ApiModelProperty(value = "工程范围及工作内容")
  private String workScopeContent;

  @ApiModelProperty(value = "合同签订金额（含税）（元）")
  private BigDecimal taxSignAmount;

  @ApiModelProperty(value = "合同签订金额（不含税）（元）")
  private BigDecimal ntaxSignAmount;

  @ApiModelProperty(value = "签订税额(初始签订金额)")
  private BigDecimal signTax;

  @ApiModelProperty(value = "合同变更金额（含税）（元）（合同变更回写）")
  private BigDecimal taxChangeAmount;

  @ApiModelProperty(value = "合同变更后金额（元）（含税）（合同签订金额+合同变更金额）")
  private BigDecimal taxChangedAmount;

  @ApiModelProperty(value = "合同变更后金额（元）（不含税）")
  private BigDecimal ntaxChangedAmount;

  @ApiModelProperty(value = "累计结算(元) （结算单回写）")
  private BigDecimal totalTaxSettleAmount;

  @ApiModelProperty(value = "累计结算金额（无税）（结算单回写）")
  private BigDecimal totalNtaxSettleAmount;

  @ApiModelProperty(value = "合同外累计结算金额（结算单回写）")
  private BigDecimal totalTaxOutConSettleAmount;

  @ApiModelProperty(value = "累计合同外结算金额（不含税）（结算单回写）")
  private BigDecimal totalNtaxOutConSettleAmount;

  @ApiModelProperty(value = "剩余合同金额(含税)(元)（=合同变更后金额 - 累计结算金额）")
  private BigDecimal remainTaxConAmount;

  @ApiModelProperty(value = "合同剩余金额（不含税）（元）")
  private BigDecimal remainNtaxConAmount;

  @ApiModelProperty(value = "实付付款金额（含税）(元)(累计实付付款金额)")
  private BigDecimal totalActualPaymentAmount;

  @ApiModelProperty(value = "按结算金额欠付金额(含税)(元)（结算金额-实付付款金额）")
  private BigDecimal settleOwedAmount;

  @ApiModelProperty(value = "完工结算状态（0-否，1-是）")
  private String whetherCompleteSettle;

  @ApiModelProperty(value = "币种：引用GB/T12406-2022表示货币的代码")
  private String moneyType;

  @ApiModelProperty(value = "发票类型（1-专票 2-普票 3-数电票）")
  private String invoiceType;

  @ApiModelProperty(value = "合同税率(%)")
  private BigDecimal taxRate;

  @ApiModelProperty(value = "约定预付款比例(%）")
  private BigDecimal imprestRatio;

  @ApiModelProperty(value = "约定预付款金额（元）")
  private BigDecimal imprestAmount;

  @ApiModelProperty(value = "预付款扣回条件")
  private String imprestDeductCondition;

  @ApiModelProperty(value = "预付款全部扣回截止点(%)")
  private BigDecimal imprestDeductPoint;

  @ApiModelProperty(value = "允许合同外结算占合同比例(%)")
  private BigDecimal outConSettleRatio;

  @ApiModelProperty(value = "是否关联我的钢铁网价格（0-否，1-是）")
  private String whetherAssociateSteelPrice;

  @ApiModelProperty(value = "市场简称（是在关联我的钢铁网价格为“是”的情况下才有值）")
  private String marketShortName;

  @ApiModelProperty(value = "我的钢铁网价格浮动值")
  private BigDecimal steelPriceFloat;

  @ApiModelProperty(value = "停滞台班结算比例(%)")
  private BigDecimal stagnateMachineSettleRatio;

  @ApiModelProperty(value = "可变更字段(JSON)")
  private String changeField;

  @ApiModelProperty(value = "是否超量")
  private Integer isExceed;

  @ApiModelProperty(value = "流程状态 0-自由态 1-审核中 2-驳回 3-弃审 4-完成")
  private String procStatus;

  @ApiModelProperty(value = "审批完成时间")
  private Date procCompleteTime;

  @ApiModelProperty(value = "最后审批人")
  private String lastProcApprover;

  @ApiModelProperty(value = "下一审批人")
  private String nextProcApprover;

  @ApiModelProperty(value = "审批流程组织id")
  private String procOrgId;

  @ApiModelProperty(value = "供应商状态 0未发送、1已发送、2已签收、3已确认")
  private String supplierStatus;

  @ApiModelProperty(value = "附件id")
  private String attachBusinessId;

  @ApiModelProperty(value = "删除状态（0-生效 其他删除）")
  private Integer valid;

}
