package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.vendor.vo.res.VendorVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同创建基本信息
 *
 * @author chenming
 * @date 2024/06/07
 */
@Data
public class AgreementCreateBaseInfoVO extends AdviceObject {
    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    @ApiModelProperty(value = "合约拆分 id")
    private Long splitId;

    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    @ApiModelProperty(value = "归属本级组织id")
    private String belongOrganizationId;

    @ApiModelProperty(value = "归属本级组织名称")
    private String belongOrganizationName;

    @ApiModelProperty(value = "归属最小核算项目")
    private String belongAccountingItem;

    @ApiModelProperty(value = "归属最小核算项目编码")
    private String belongAccountingItemCode;

    @ApiModelProperty(value = "交易标的物")
    private String subjectMatterName;

    @ApiModelProperty(value = "甲方机构id")
    private String partyAOrgId;

    @ApiModelProperty(value = "甲方纳税识别号")
    private String taxpayerNo;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "业务类型")
    private Integer businessType;

    @ApiModelProperty(value = "支出业务分类")
    private String expenditureBusinessType;

    @ApiModelProperty(value = "乙方法人代表")
    private String partyBLegalName;

    @ApiModelProperty(value = "乙方法人代表身份证")
    private String partyBLegalIdCard;

    @ApiModelProperty(value = "乙方法人代表联系方式")
    private String partyBLegalPhone;

    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

    @ApiModelProperty(value = "国家地区代码(履行地)")
    private String agreementPerformCountry;

    @ApiModelProperty(value = "行政区划代码(履行地)")
    private String agreementPerformDistrict;

    @ApiModelProperty(value = "合同总价（含税）")
    private BigDecimal totalAmountIncTax;

    @ApiModelProperty(value = "合同总价（不含税）")
    private BigDecimal totalAmountExcTax;

    @ApiModelProperty(value = "合同模板附件 id")
    private Long attachmentId;

    @ApiModelProperty(value = "交易标的物类型")
    private Integer subjectMatterType;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @MoneyFormat(filedName = "totalAmountIncTax",scale = 2)
    @ApiModelProperty(value = "合同总价（含税）")
    private String totalAmountIncTaxText;

    @MoneyFormat(filedName = "totalAmountExcTax",scale = 2)
    @ApiModelProperty(value = "合同总价（不含税）")
    private String totalAmountExcTaxText;

    @ApiModelProperty(value = "合同清单")
    private List<VendorBiddingListQuotationListVO> biddingListQuotation;

    @ApiModelProperty(value = "易料采购合同id")
    private String marketMaterialContractId;

    @ApiModelProperty(value = "乙方现场实际履职负责人")
    private String partyBResponsibleName;

    @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
    private String partyBResponsibleIdCard;

    @ApiModelProperty(value = "乙方现场实际履职负责人联系方式")
    private String partyBResponsiblePhone;

    @ApiModelProperty(value = "是否关联我的钢铁网价格-是否显示")
    private String isRelatedMySteelView;

    @ApiModelProperty(value = "供应商基本信息")
    private VendorVO vendorVO;
}
