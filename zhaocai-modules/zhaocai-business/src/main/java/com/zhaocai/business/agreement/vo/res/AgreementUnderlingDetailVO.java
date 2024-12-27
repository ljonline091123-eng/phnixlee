package com.zhaocai.business.agreement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.domain.AgreementPartyInfoVO;
import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class AgreementUnderlingDetailVO {
    /**
     * 合同名称
     */
    private String agreementName;

    /**
     * 合同编码
     */
    private String agreementCode;

    /**
     * 合同状态
     */
    private Integer agreementState;

    /**
     * 业务类型
     */
    private String expenditureBusinessType;


    /**
     * 合同计日工信息
     */
    private List<AgreementDailyWageVO> contractDatallerList;

    /**
     * 合同押金、保证金信息
     */
    private List<AgreementDepositVO> contractDepositList;

    /**
     * 劳务合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListLaborList;

    /**
     * 设备租赁合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListLeasedDeviceList;

    /**
     * 周材租赁合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListLeasedMaterialsList;

    /**
     * 物资合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListMaterialsList;

    /**
     * 其他合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListOtherList;

    /**
     * 专业合同清单列表
     */
    private List<AgreementUnderlingMaterialsVO> contractListSpecialtyList;

    /**
     * 机械台班列表
     */
    private List<AgreementMachineShiftVO> contractMechanicalTableList;

    /**
     * 合同-结算与付款信息节点
     */
    private List<AgreementPaymentListVO> contractNodeList;

    /**
     * 合同-结算与付款信息节点
     */
    private List<AgreementPartyInfoVO> contractPartyInfoList;

    /**
     * 甲供设备清单列表
     */
    private List<AgreementEquipmentSupplyVO> contractSupplyEquipmentList;

    /**
     * 甲供材料清单列表
     */
    private List<AgreementMaterialSupplyVO> contractSupplyMaterialsList;

    /**
     * 下浮比例(%)
     */
    private BigDecimal discountRatio;

    /**
     * 工期
     */
    private String duration;

    /**
     * 完工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date finishDate;

    /**
     * 合同 id
     */
    private Long agreementId;

    /**
     * 约定预付款金额
     */
    private BigDecimal prepaymentAmount;

    /**
     * 预付款扣回条件
     */
    private String prepaymentDeductionConditions;

    /**
     * 预付款全部扣回截止点
     */
    private BigDecimal prepaymentDeductionDeadline;

    /**
     * 约定预付款比例
     */
    private BigDecimal prepaymentRatio;

    /**
     * 单位内部合同管理编码
     */
    private String innerAgreementCode;

    /**
     * 发票类型
     */
    private String invoiceType;

    /**
     * 币种
     */
    private String currency;

    /**
     * 允许合同外结算占合同比例
     */
    private BigDecimal outOfSettlementRatio;

    /**
     * 甲方 id
     */
    private String partyAOrgId;

    /**
     * 甲方名称
     */
    private String partyAName;

    /**
     * 乙方名称
     */
    private String partyBName;

    /**
     * 供应商 id
     */
    private Long vendorId;

    /**
     * 乙方法人代表
     */
    private String partyBLegalName;

    /**
     * 乙方法人代表身份证
     */
    private String partyBLegalIdCard;

    /**
     * 乙方法人代表联系方式
     */
    private String partyBLegalPhone;

    /**
     * 乙方现场实际履职负责人
     */
    private String partyBResponsibleName;

    /**
     * 乙方现场实际履职负责人身份证
     */
    private String partyBResponsibleIdCard;

    /**
     * 乙方现场实际履职负责人联系方式
     */
    private String partyBResponsiblePhone;

    /**
     * 支付周期
     */
    private String paymentCycle;

    /**
     * 支付方式
     */
    private String paymentWay;

    /**
     * 价格形式
     */
    private String priceForm;

    /**
     * 合约规划 id
     */
    private String contractId;

    /**
     * 合约规划名称
     */
    private String contractName;

    /**
     * 最小核算项目编号
     */
    private String projectCode;

    /**
     * 合同签订日期
     */
    private Date agreementSignDate;

    /**
     * 计租方式
     */
    private Integer rentalMethod;

    /**
     * 停滞台班结算比例
     */
    private BigDecimal stagnationRatio;

    /**
     * 进场日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date entryDate;

    /**
     * 我的钢铁网价格浮动值
     */
    private BigDecimal mySteelPriceFluctuation;

    /**
     * 合同税率
     */
    private BigDecimal contractTaxRate;

    /**
     * 合同签订金额(含税)
     */
    private BigDecimal totalAmountIncTax;

    /**
     * 合同签订金额(不含税)
     */
    private BigDecimal totalAmountExcTax;

    /**
     * 是否关联我的钢铁网价格
     */
    private Integer isRelatedMySteel;

    /**
     * 工程范围及工作内容
     */
    private String scopeOfWork;

    /**
     * 外部数据类型 0其它 1钢筋采购 2商品砼采购
     */
    private String externalType;

    /**
     * 价格类型  1:固定价  2:浮动价
     */
    private Integer priceType;


    public AgreementUnderlingDetailVO(Agreement agreement, AgreementPaymentItemVO agreementPaymentItem, ContractPlanning contractPlanning,
                                      ProcurementScheme procurementScheme) {
        this.agreementName = agreement.getAgreementName();
        this.agreementCode = agreement.getAgreementCode();
        this.agreementState = null;
        this.expenditureBusinessType = ProcurementPlanTypeConver.converFromProcurementPlanType(agreement.getExpenditureBusinessType());
        this.discountRatio = agreement.getDiscountRatio();
        this.duration = agreement.getDuration();
        this.finishDate = agreement.getFinishDate();
        this.agreementId = agreement.getId();
        this.prepaymentAmount = agreementPaymentItem.getPrepaymentAmount();
        this.prepaymentDeductionConditions = agreementPaymentItem.getPrepaymentDeductionConditions();
        this.prepaymentDeductionDeadline = agreementPaymentItem.getPrepaymentDeductionDeadline();
        this.prepaymentRatio = agreementPaymentItem.getPrepaymentRatio();
        this.innerAgreementCode = agreement.getInnerAgreementCode();
        this.invoiceType = agreementPaymentItem.getInvoiceType();
        this.currency = agreementPaymentItem.getCurrency();
        this.outOfSettlementRatio = agreementPaymentItem.getOutOfSettlementRatio();

        this.partyAOrgId = agreement.getPartyAOrgId();
        this.partyAName = agreement.getPartyAName();
        this.vendorId = agreement.getVendorId();
        this.partyBLegalName = agreement.getPartyBLegalName();
        this.partyBLegalIdCard = agreement.getPartyBLegalIdCard();
        this.partyBLegalPhone = agreement.getPartyBLegalPhone();
        this.partyBName = agreement.getPartyBName();
        this.partyBResponsibleName = agreement.getPartyBResponsibleName();
        this.partyBResponsibleIdCard = agreement.getPartyBResponsibleIdCard();
        this.partyBResponsiblePhone = agreement.getPartyBResponsiblePhone();
        this.paymentCycle = agreement.getPaymentCycle();
        this.paymentWay = agreement.getPaymentWay();
        this.priceForm = agreement.getPriceForm();
        this.rentalMethod = agreement.getRentalMethod();
        this.agreementSignDate = null;
        this.stagnationRatio = agreementPaymentItem.getStagnationRatio();
        this.entryDate = agreement.getEntryDate();
        this.mySteelPriceFluctuation = agreementPaymentItem.getMySteelPriceFluctuation();
        this.contractTaxRate = agreementPaymentItem.getContractTaxRate();
        this.totalAmountIncTax = agreement.getTotalAmountIncTax();
        this.totalAmountExcTax = agreement.getTotalAmountExcTax();
        this.isRelatedMySteel = agreementPaymentItem.getIsRelatedMySteel();
        this.scopeOfWork = agreement.getScopeOfWork();

        this.contractId = contractPlanning.getContractPlanningId();
        this.contractName = contractPlanning.getContractPlanningName();
        this.projectCode = contractPlanning.getProjectCode();

        //this.externalType = procurementScheme.getSubjectMatter();
        this.priceType = procurementScheme.getPriceType();
    }
}
