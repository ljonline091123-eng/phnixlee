package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 易料市集物料清单
 * </p>
 *
 * @author susiyuan
 * @since 2024-09-21
 */
@Getter
@Setter
@TableName("tb_market_materials_list")
@ApiModel(value = "MarketMaterialsList对象", description = "易料市集物料清单")
public class MarketMaterialsList extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商务策划id")
    private String planId;

    @ApiModelProperty("清单名称")
    private String materialsName;

    @ApiModelProperty("清单编码")
    private String materialsCode;

    @ApiModelProperty("规格型号")
    private String category;

    @ApiModelProperty("单位")
    private String unitName;

    @ApiModelProperty("数量")
    private BigDecimal quantity;

    @ApiModelProperty("含税单价")
    private BigDecimal price;

    @ApiModelProperty("不含税单价")
    private BigDecimal notTaxPrice;

    @ApiModelProperty("税率")
    private Integer taxRate;

    @ApiModelProperty("易料单价")
    private BigDecimal offerPrice;

    @ApiModelProperty("易料品牌")
    private String brand;

    @ApiModelProperty("易料编码")
    private String goodsCode;

    @ApiModelProperty("易料商品名称")
    private String goodsName;

    @ApiModelProperty("易料供应商编码")
    private String supplyCode;

    @ApiModelProperty("易料供应商名称")
    private String supplyName;


}
