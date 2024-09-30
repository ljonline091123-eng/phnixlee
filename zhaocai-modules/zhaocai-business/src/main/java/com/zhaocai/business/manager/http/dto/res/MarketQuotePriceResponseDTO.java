package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/9/20 11:23
 * @description 清单报价列表数据
 */
@Data
public class MarketQuotePriceResponseDTO {

    @ApiModelProperty(value = "商务策划id")
    private String planId;

    @ApiModelProperty(value = "清单名称")
    private String quoteName;

    @ApiModelProperty(value = "清单编码")
    private String quoteNo;

    @ApiModelProperty(value = "报价状态")
    private Integer status;

    @ApiModelProperty(value = "规格型号")
    private String category;

    @ApiModelProperty(value = "单位")
    private String unitName;

    @ApiModelProperty(value = "数量")
    private BigDecimal quantity;

    @ApiModelProperty(value = "含税单价")
    private BigDecimal price;

    @ApiModelProperty(value = "不含税单价")
    private BigDecimal noTaxPrice;

    @ApiModelProperty(value = "税率")
    private Integer taxRate;

    @ApiModelProperty(value = "易料市集含税单价")
    private BigDecimal offerPrice;

    @ApiModelProperty(value = "易料市集品牌")
    private String offerBrand;

    @ApiModelProperty(value = "易料市集商品编码")
    private String offerGoodsCode;

    @ApiModelProperty(value = "易料市集供应商名称")
    private String offerSupplierName;

    @ApiModelProperty(value = "易料市集供应商编码")
    private String offerSupplierCode;

    @ApiModelProperty(value = "易料市集商品名")
    private String goodsName;

}
