package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/20 17:20
 */
@Data
public class MarketMaterialListQuoteRequestDTO {

    @ApiModelProperty(value = "合同主键ID")
    private String id;

    @ApiModelProperty(value = "计划id")
    private String planId;

    @ApiModelProperty(value = "归属最小核算项目")
    private String belongAccountingItem;

    @ApiModelProperty(value = "归属最小核算项目编码")
    private String belongAccountingItemCode;

    @ApiModelProperty(value = "合同编码")
    private String agreementCode;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "乙方(供应商) Id")
    private String vendorId;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "支出业务分类1.购买材料2.租赁材料3.租赁机械(设备)4.专业分包5.劳务分包 6.其他")
    private String expenditureBusinessType;

    @ApiModelProperty(value = "乙方法人代表")
    private String partyBLegalName;

    @ApiModelProperty(value = "乙方法人身份证")
    private String partyBLegalIdCard;

    @ApiModelProperty(value = "乙方法人联系方式")
    private String partyBLegalPhone;

    @ApiModelProperty(value = "乙方现场实际履职负责人")
    private String partyBResponsibleName;

    @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
    private String partyBResponsibleIdCard;

    @ApiModelProperty(value = "乙方现场实际履职负责人联系方式")
    private String partyBResponsiblePhone;

    @ApiModelProperty(value = "国家地区代码(履行地)")
    private String agreementPerformCountry;

    @ApiModelProperty(value = "行政区划代码(履行地)")
    private String agreementPerformDistrict;

    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

    List<QuotePriceItem> list;

}
