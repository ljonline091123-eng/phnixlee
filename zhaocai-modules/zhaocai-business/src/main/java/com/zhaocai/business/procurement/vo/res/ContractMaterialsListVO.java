package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.manager.http.dto.res.ContractPlanMaterialListDTO;
import com.zhaocai.common.core.utils.NumberUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合约清单
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "合约规划物料清单")
@NoArgsConstructor
public class ContractMaterialsListVO extends AdviceObject {

    @ApiModelProperty(value = "物料 id")
    private String materialsId;

    @ApiModelProperty(value = "物料清单唯一 id")
    private String materialsUniqueId;

    @ApiModelProperty(value = "物料清单编码")
    private String materialsCode;

    @ApiModelProperty(value = "物料清单名称")
    private String materialsName;

    @ApiModelProperty(value = "物料清单名称(导入)")
    private String materialsNameImport;

    @ApiModelProperty(value = "规格型号")
    private String specification;

    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

    @ApiModelProperty(value = "成本科目档案ID")
    private String costAccountId;

    @ApiModelProperty(value = "成本科目编码")
    private String costAccountCode;

    @ApiModelProperty(value = "成本科目名称")
    private String costAccountName;

    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value = "税率编码")
    private String taxRateCode;


    @DictCache(dictBizEnum = DictBizEnum.RAX_ARCHIVES,filedName = "taxRateCode")
    @ApiModelProperty(value = "税率名称")
    private String taxRateName;

    @ApiModelProperty(value = "单价(不含税)")
    private BigDecimal unitPriceExclTax;

    @ApiModelProperty(value = "单价(含税) ")
    private BigDecimal unitPriceInclTax;

    @ApiModelProperty(value = "金额(不含税)")
    private BigDecimal amountExclTax;

    @ApiModelProperty(value = "金额(含税)")
    private BigDecimal amountInclTax;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @ApiModelProperty(value = "税额")
    private BigDecimal taxAmount;

    @ApiModelProperty(value = "工程量")
    private BigDecimal quantity;

    @ApiModelProperty(value = "已使用数量")
    private BigDecimal usedCount;

    @ApiModelProperty(value = "剩余使用数量")
    private BigDecimal surplusQuantity;

    @ApiModelProperty(value = "转换数量")
    private BigDecimal transferQuantity;

    @ApiModelProperty(value = "数量")
    private BigDecimal count;

    @ApiModelProperty(value = "租赁方式")
    private String rentMode;

    @ApiModelProperty(value = "租赁时间")
    private BigDecimal rentTime;

    @ApiModelProperty(value = "租赁数量")
    private BigDecimal rentQuantity;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "交易标的物标识")
    private String subjectMatterFlag;

    @MoneyFormat(filedName = "unitPriceExclTax")
    @ApiModelProperty(value = "单价(不含税)")
    private String unitPriceExclTaxText;

    @MoneyFormat(filedName = "unitPriceInclTax")
    @ApiModelProperty(value = "单价(含税) ")
    private String unitPriceInclTaxText;

    @MoneyFormat(filedName = "taxRate",scale = 2)
    @ApiModelProperty(value = "税率")
    private String taxRateText;


    @MoneyFormat(filedName = "amountExclTax",scale = 2)
    @ApiModelProperty(value = "金额(不含税)")
    private String amountExclTaxText;

    @MoneyFormat(filedName = "amountInclTax",scale = 2)
    @ApiModelProperty(value = "金额(含税)")
    private String amountInclTaxText;

    @MoneyFormat(filedName = "taxAmount",scale = 2)
    @ApiModelProperty(value = "税额")
    private String taxAmountText;

    @MoneyFormat(filedName = "quantity")
    @ApiModelProperty(value = "工程量")
    private String quantityText;

    @MoneyFormat(filedName = "usedCount")
    @ApiModelProperty(value = "已使用数量")
    private String usedCountText;

    @MoneyFormat(filedName = "surplusQuantity")
    @ApiModelProperty(value = "剩余使用数量")
    private String surplusQuantityText;

    @MoneyFormat(filedName = "transferQuantity")
    @ApiModelProperty(value = "转换数量")
    private String transferQuantityText;

    @MoneyFormat(filedName = "basePrice")
    @ApiModelProperty(value = "基价")
    private String basePriceText;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "商品编号")
    private String code;

    @ApiModelProperty(value = "商品名称")
    private String name;

    @ApiModelProperty(value = "商品规格")
    private String category;

    @ApiModelProperty(value = "商品单位")
    private String unitName;

    @ApiModelProperty(value = "商品数量")
    private BigDecimal goodsQuantity;

    @ApiModelProperty(value = "易料市集含税单价")
    private BigDecimal offerPrice;

    @ApiModelProperty(value = "易料市集品牌")
    private String offerBrand;

    @ApiModelProperty(value = "易料市集商品id")
    private String skuId;

    @ApiModelProperty(value = "总价(含税)")
    private BigDecimal totalPrice;

    @MoneyFormat(filedName = "totalPrice",scale = 2)
    @ApiModelProperty(value = "总价(含税)")
    private String totalPriceText;

    @ApiModelProperty(value = "浮动率")
    private BigDecimal floatingRate;

    @MoneyFormat(filedName = "floatingRate",scale = 2)
    @ApiModelProperty(value = "浮动率")
    private String floatingRateText;

    @ApiModelProperty(value = "备注")
    private String remark;

    public ContractMaterialsListVO(ContractPlanMaterialListDTO dto,Integer procurementType) {
        this.materialsId = dto.getId();
        this.materialsUniqueId = dto.getSubjectDtlUniqueId();
        this.materialsCode = dto.getSubjectDtlCode();
        this.materialsName = dto.getSubjectDtlName();
        this.materialsNameImport = dto.getSubjectDtlNameImport();
        this.specification = dto.getSpecs();
        this.measurementRules = dto.getMetrologicalRules();
        this.unitMeasurement = dto.getMeasureUnit();
        this.workContent = dto.getBasicJob();
        this.costAccountId = dto.getSubjectId();
        this.costAccountCode = dto.getSubjectCode();
        this.costAccountName = dto.getSubjectName();
        this.taxRate = dto.getTaxRate();
        this.taxRateCode = dto.getTaxRateCode();
        this.unitPriceExclTax = dto.getNtaxPrice();
        this.unitPriceInclTax = dto.getTaxPrice();
        this.amountExclTax = dto.getNtaxAmount();
        this.amountInclTax = dto.getTaxAmount();
        this.taxAmount = dto.getTax();
        this.basePrice = dto.getTaxPrice();

        this.rentMode = dto.getRentMode();
        this.rentTime = dto.getRentTime();
        this.rentQuantity = dto.getRentQuantity();

        this.quantity = dto.getQuantity();
        this.usedCount = dto.getUsedQuantity();
        this.surplusQuantity = dto.getSurplusQuantity();
        this.transferQuantity = dto.getTransferQuantity();

        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(procurementType) || ProcurementPlanTypeEnum.LEASED_MATERIAL.equalsType(procurementType)) {
            this.count = NumberUtil.subtract(dto.getTransferQuantity(),dto.getUsedQuantity());
        } else {
            this.count = NumberUtil.subtract(dto.getQuantity(),dto.getUsedQuantity());
        }
    }
}
