package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author lsn
 * @date 2024/10/17 17:20
 */
@Data
public class QuotePriceItem {

    @ApiModelProperty(value = "清单id")
    private String requireId;

    @ApiModelProperty(value = "清单名称")
    private String quoteName;

    @ApiModelProperty(value = "清单编码")
    private String quoteNo;

    @ApiModelProperty(value = "报价状态  0:未报价,1:已报价,2:不报价")
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

    @ApiModelProperty(value = "创建日期")
    private Date createdTime;

    @ApiModelProperty(value = "更新时间")
    private Date updatedTime;

    @ApiModelProperty(value = "易料市集供应商名称")
    private String offerSupplierName;

    @ApiModelProperty(value = "易料市集供应商编码")
    private String offerSupplierCode;

    @ApiModelProperty(value = "易料市集商品名")
    private String goodsName;

}
