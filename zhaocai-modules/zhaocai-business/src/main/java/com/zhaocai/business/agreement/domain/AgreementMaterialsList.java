package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同物料清单对象 tb_agreement_materials_list
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
@TableName(value = "tb_agreement_materials_list")
public class AgreementMaterialsList extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 合同id
     */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /**
     * 物料清单id
     */
    @ApiModelProperty(value = "物料清单id")
    private Long materialsListId;

    /**
     * 投标清单报价 id
     */
    @ApiModelProperty(value = "投标清单报价 id")
    private Long biddingListQuotationId;

    /**
     * 合约拆分 id
     */
    @ApiModelProperty(value = "合约拆分 id")
    private Long contractSplitId;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    private String materialsCode;

    /**
     * 物料名称
     */
    @ApiModelProperty(value = "物料名称")
    private String materialsName;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌")
    private String brand;

    /**
     * 价款类型
     */
    @ApiModelProperty(value = "价款类型")
    private String paymentType;

    /**
     * 租赁方式
     */
    @ApiModelProperty(value = "租赁方式")
    private String rentalType;

    /**
     * 计租单位
     */
    @ApiModelProperty(value = "计租单位")
    private String rentalUnit;

    /**
     * 租赁时间
     */
    @ApiModelProperty(value = "租赁时间")
    private String rentalDuration;

    /**
     * 工作量
     */
    @ApiModelProperty(value = "工作量")
    private String workload;

    /**
     * 签订数量
     */
    @ApiModelProperty(value = "签订数量")
    private BigDecimal signCount;

    /**
     * 签订税率
     */
    @ApiModelProperty(value = "签订税率")
    private BigDecimal signTaxRate;

    /**
     * 签订单价（含税）
     */
    @ApiModelProperty(value = "签订单价（含税）")
    private BigDecimal signUnitPriceInclTax;

    /**
     * 签订单价（不含税）
     */
    @ApiModelProperty(value = "签订单价（不含税）")
    private BigDecimal signUnitPriceExclTax;

    /**
     * 签订金额（含税）
     */
    @ApiModelProperty(value = "签订金额（含税）")
    private BigDecimal signAmountInclTax;

    /**
     * 签订金额（不含税）
     */
    @ApiModelProperty(value = "签订金额（不含税）")
    private BigDecimal signAmountExclTax;

    /**
     * 供应商招标税率
     */
    @ApiModelProperty(value = "供应商招标税率")
    private BigDecimal vendorTaxRate;

    /**
     * 供应商招标单价（含税）
     */
    @ApiModelProperty(value = "供应商招标单价（含税）")
    private BigDecimal vendorUnitPriceInclTax;

    /**
     * 供应商招标单价（不含税）
     */
    @ApiModelProperty(value = "供应商招标单价（不含税）")
    private BigDecimal vendorUnitPriceExclTax;

    /**
     * 供应商招标金额（含税）
     */
    @ApiModelProperty(value = "供应商招标金额（含税）")
    private BigDecimal vendorAmountInclTax;

    /**
     * 供应商招标金额（不含税）
     */
    @ApiModelProperty(value = "供应商招标金额（不含税）")
    private BigDecimal vendorAmountExclTax;


    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "易料市集商品编码")
    private String offerGoodsCode;

    @ApiModelProperty(value = "易料市集商品名")
    private String goodsName;

    @ApiModelProperty(value = "易料市集品牌")
    private String offerBrand;

    @ApiModelProperty(value = "易料市集含税单价")
    private BigDecimal offerPrice;
}
