package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同基本信息对象 tb_agreement
 *
 * @author chenming
 * @date 2024-06-05
 */
@Data
@TableName(value = "tb_agreement")
@EqualsAndHashCode(callSuper = true)
public class Agreement extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购方案 id
     */
    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    /**
     * 供应商 id
     */
    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    /**
     * 合约规划拆分id
     */
    @ApiModelProperty(value = "合约规划拆分id")
    private Long contractSplitId;

    /**
     * 归属本级组织id
     */
    @ApiModelProperty(value = "归属本级组织id")
    private String belongOrganizationId;

    /**
     * 归属本级组织名称
     */
    @ApiModelProperty(value = "归属本级组织名称")
    private String belongOrganizationName;

    /**
     * 归属最小核算项目
     */
    @ApiModelProperty(value = "归属最小核算项目")
    private String belongAccountingItem;

    /**
     * 归属最小核算项目编码
     */
    @ApiModelProperty(value = "归属最小核算项目编码")
    private String belongAccountingItemCode;

    /**
     * 合同名称
     */
    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    /**
     * 合同编号
     */
    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    /**
     * 合同签订金额(含税)
     */
    @ApiModelProperty(value = "合同签订金额(含税)")
    private BigDecimal totalAmountIncTax;

    /**
     * 合同签订金额(不含税)
     */
    @ApiModelProperty(value = "合同签订金额(不含税)")
    private BigDecimal totalAmountExcTax;

    /**
     * 单位内部合同管理编码
     */
    @ApiModelProperty(value = "单位内部合同管理编码")
    private String innerAgreementCode;

    /**
     * 甲方机构id
     */
    @ApiModelProperty(value = "甲方机构id")
    private String partyAOrgId;

    /**
     * 甲方名称
     */
    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    /**
     * 乙方名称
     */
    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    /**
     * 支出业务分类 {@link com.zhaocai.business.common.enums.ProcurementPlanTypeEnum}
     */
    @ApiModelProperty(value = "支出业务分类")
    private Integer expenditureBusinessType;

    /**
     * 交易标的物编码
     */
    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    /**
     * 交易标的物名称
     */
    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    /**
     * 支付周期
     */
    @ApiModelProperty(value = "支付周期")
    private String paymentCycle;

    /**
     * 支付方式
     */
    @ApiModelProperty(value = "支付方式")
    private String paymentWay;

    /**
     * 填报人
     */
    @ApiModelProperty(value = "填报人")
    private String reporterName;

    /**
     * 甲方联系人名称
     */
    @ApiModelProperty(value = "甲方联系人名称")
    private String partyAContactName;

    /**
     * 甲方联系人电话
     */
    @ApiModelProperty(value = "甲方联系人电话")
    private String partyAContactPhone;


    /**
     * 乙方法人代表
     */
    @ApiModelProperty(value = "乙方法人代表")
    private String partyBLegalName;

    /**
     * 乙方法人代表身份证
     */
    @ApiModelProperty(value = "乙方法人代表身份证")
    private String partyBLegalIdCard;

    /**
     * 乙方法人代表联系方式
     */
    @ApiModelProperty(value = "乙方法人代表联系方式")
    private String partyBLegalPhone;

    /**
     * 乙方现场实际履职负责人
     */
    @ApiModelProperty(value = "乙方现场实际履职负责人")
    private String partyBResponsibleName;

    /**
     * 乙方现场实际履职负责人身份证
     */
    @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
    private String partyBResponsibleIdCard;

    /**
     * 乙方现场实际履职负责人联系方式
     */
    @ApiModelProperty(value = "乙方现场实际履职负责人联系方式")
    private String partyBResponsiblePhone;

    /**
     * 合同状态
     */
    @ApiModelProperty(value = "合同状态")
    private Integer agreementState;

    /**
     * 合同签订状态
     */
    @ApiModelProperty(value = "合同签订状态")
    private Integer agreementSignState;

    /**
     * 合同签订日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同签订日期")
    private Date agreementSignDate;

    /**
     * 合同生效日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同生效日期")
    private Date agreementEffectiveDate;

    /**
     * 合同履行地
     */
    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

    /**
     * 国家地区代码(履行地)
     */
    @ApiModelProperty(value = "国家地区代码(履行地)")
    private String agreementPerformCountry;

    /**
     * 行政区划代码(履行地)
     */
    @ApiModelProperty(value = "行政区划代码(履行地)")
    private String agreementPerformDistrict;

    /**
     * 计租方式
     */
    @ApiModelProperty(value = "计租方式")
    private Integer rentalMethod;

    /**
     * 进场日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "进场日期")
    private Date entryDate;

    /**
     * 完工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "完工日期")
    private Date finishDate;

    /**
     * 工期
     */
    @ApiModelProperty(value = "工期")
    private String duration;

    /**
     * 价格形式
     */
    @ApiModelProperty(value = "价格形式")
    private String priceForm;

    /**
     * 下浮比例(%)
     */
    @ApiModelProperty(value = "下浮比例(%)")
    private BigDecimal discountRatio;

    /**
     * 工程范围及工作内容
     */
    @ApiModelProperty(value = "工程范围及工作内容")
    private String scopeOfWork;

    /**
     * 合同履行开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同履行开始日期")
    private Date contractStartDate;

    /**
     * 合同履行结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同履行结束日期")
    private Date contractEndDate;

    /**
     * 合同附件 id
     */
    @ApiModelProperty(value = "合同附件 id")
    private Long attachmentId;

    /**
     * 合同标签附件 id
     */
    @ApiModelProperty(value = "合同标签附件 id")
    private Long labelAttachmentId;

    /**
     * 合同水印附件 id
     */
    @ApiModelProperty(value = "合同水印附件 id")
    private Long watermarkAttachmentId;

    /**
     * pdf 附件 id
     */
    @ApiModelProperty(value = "pdf 附件 id")
    private Long convertPdfAttachmentId;

    /**
     * 电子签章附件 id
     */
    @ApiModelProperty(value = "电子签章附件 id")
    private Long signAttachmentId;

    /**
     * 合同签署用户
     */
    @ApiModelProperty(value = "合同签署用户")
    private Long signatureUserId;

    /**
     * 作废原因
     */
    @ApiModelProperty(value = "作废原因")
    private String cancelledReason;

    /**
     * 流程实例id
     */
    @ApiModelProperty(hidden = true)
    private String wfProcessId;

    /**
     * 易料采购合同id
     */
    @ApiModelProperty(value = "易料采购合同id")
    private String marketMaterialContractId;

    /**
     * 易料采购合同招标编码
     */
    @ApiModelProperty(value = "易料采购合同招标编码")
    private String procurementSchemeCode;

}
