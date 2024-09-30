package com.zhaocai.business.agreement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AgreementVO extends AdviceObject {

    @ApiModelProperty(value = "主键 id")
    private Long id;

    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    @ApiModelProperty(value = "合约规划拆分id")
    private Long contractSplitId;

    @ApiModelProperty(value = "归属本级组织id")
    private String belongOrganizationId;

    @ApiModelProperty(value = "归属本级组织名称")
    private String belongOrganizationName;

    @ApiModelProperty(value = "归属最小核算项目")
    private String belongAccountingItem;

    @ApiModelProperty(value = "归属最小核算项目编码")
    private String belongAccountingItemCode;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "合同签订金额(含税)")
    private BigDecimal totalAmountIncTax;

    @ApiModelProperty(value = "合同签订金额(不含税)")
    private BigDecimal totalAmountExcTax;

    @ApiModelProperty(value = "单位内部合同管理编码")
    private String innerAgreementCode;

    @ApiModelProperty(value = "甲方机构id")
    private String partyAOrgId;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "支出业务分类")
    private Integer expenditureBusinessType;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "支付周期")
    private String paymentCycle;

    @ApiModelProperty(value = "支付方式")
    private String paymentWay;

    @ApiModelProperty(value = "填报人")
    private String reporterName;

    @ApiModelProperty(value = "乙方法人代表")
    private String partyBLegalName;

    @ApiModelProperty(value = "乙方法人代表身份证")
    private String partyBLegalIdCard;

    @ApiModelProperty(value = "乙方法人代表联系方式")
    private String partyBLegalPhone;

    @ApiModelProperty(value = "乙方现场实际履职负责人")
    private String partyBResponsibleName;

    @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
    private String partyBResponsibleIdCard;

    @ApiModelProperty(value = "乙方现场实际履职负责人联系方式")
    private String partyBResponsiblePhone;

    @ApiModelProperty(value = "合同状态")
    private Integer agreementState;

    @ApiModelProperty(value = "合同签订状态")
    private Integer agreementSignState;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同签订日期")
    private Date agreementSignDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同生效日期")
    private Date agreementEffectiveDate;

    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

    @ApiModelProperty(value = "国家地区代码(履行地)")
    private String agreementPerformCountry;

    @ApiModelProperty(value = "行政区划代码(履行地)")
    private String agreementPerformDistrict;

    @ApiModelProperty(value = "计租方式")
    private Integer rentalMethod;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "进场日期")
    private Date entryDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "完工日期")
    private Date finishDate;

    @ApiModelProperty(value = "工期")
    private String duration;

    @ApiModelProperty(value = "价格形式")
    private String priceForm;

    @ApiModelProperty(value = "下浮比例(%)")
    private BigDecimal discountRatio;

    @ApiModelProperty(value = "工程范围及工作内容")
    private String scopeOfWork;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同履行开始日期")
    private Date contractStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同履行结束日期")
    private Date contractEndDate;

    @ApiModelProperty(value = "任务编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "任务名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = " 签约单位纳税人识别号")
    private String identificationNumber;

    @ApiModelProperty(value = "甲方联系人电话")
    private String partyAContactPhone;

    @ApiModelProperty(value = "甲方联系人名称")
    private String partyAContactName;

    @ApiModelProperty(value = "乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContactName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "合同附件名称")
    private String attachmentName;

    @ApiModelProperty(value = "审批流程 id")
    private String wfProcessId;

    @ApiModelProperty(value = "支付周期")
    private String paymentCycleText;

    @ApiModelProperty(value = "支付方式")
    private String paymentWayText;

    @ApiModelProperty(value = "支出业务分类")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "expenditureBusinessType")
    private String expenditureBusinessTypeText;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_RENTAL_METHOD,filedName = "rentalMethod")
    @ApiModelProperty(value = "计租方式")
    private String rentalMethodText;

    @MoneyFormat(filedName = "totalAmountIncTax",scale = 2)
    @ApiModelProperty(value = "合同签订金额(含税)")
    private String totalAmountIncTaxText;

    @MoneyFormat(filedName = "totalAmountExcTax",scale = 2)
    @ApiModelProperty(value = "合同签订金额(不含税)")
    private String totalAmountExcTaxText;

    @ApiModelProperty(value = "合同 pdf 附件 id")
    private Long pdfAttachmentId;

    @ApiModelProperty(value = "价格形式")
    private String priceFormText;

    @ApiModelProperty(value = "交易标的物")
    private String subjectMatter;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;


    @ApiModelProperty(value = "是否可操作")
    private Integer isOperate;

    @ApiModelProperty(value = "甲方机构")
    private Long partyADeptId;

    @ApiModelProperty(value ="合同标签附件Url")
    private String labelAttachmentUrl;
}
