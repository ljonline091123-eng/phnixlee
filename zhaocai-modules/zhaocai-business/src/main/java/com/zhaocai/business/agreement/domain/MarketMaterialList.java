package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 易料采购清单信息对象 tb_market_material_list
 *
 * @author lsn
 * @date 2024-10-22
 */
@Data
@TableName(value = "tb_market_material_list")
public class MarketMaterialList {

    /**
     * 清单 id
     */
    @TableId
    @ApiModelProperty(value = "清单 id")
    private String requireId;

    /**
     * 合同 id
     */
    @ApiModelProperty(value = "合同 id")
    private String contractId;

    /**
     * 清单名称
     */
    @ApiModelProperty(value = "清单名称")
    private String quoteName;

    /**
     * 清单编码
     */
    @ApiModelProperty(value = "清单编码")
    private String quoteNo;

    /**
     * 报价状态
     */
    @ApiModelProperty(value = "报价状态")
    private Integer status;

    /**
     * 规格型号
     */
    @ApiModelProperty(value = "规格型号")
    private String category;

    /**
     * 单位
     */
    @ApiModelProperty(value = "单位")
    private String  unitName;

    /**
     * 数量
     */
    @ApiModelProperty(value = "数量")
    private BigDecimal quantity;

    /**
     * 含税单价
     */
    @ApiModelProperty(value = "含税单价")
    private BigDecimal price;

    /**
     * 不含税单价
     */
    @ApiModelProperty(value = "不含税单价")
    private BigDecimal noTaxPrice;

    /**
     * 税率
     */
    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    /**
     * 易料市集含税单价
     */
    @ApiModelProperty(value = "易料市集含税单价")
    private BigDecimal offerPrice;

    /**
     * 易料市集品牌
     */
    @ApiModelProperty(value = "易料市集品牌")
    private String offerBrand;

    /**
     * 易料市集商品编码
     */
    @ApiModelProperty(value = "易料市集商品编码")
    private String offerGoodsCode;

    /**
     * 创建日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建日期")
    private Date createdTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updatedTime;

    /**
     * 易料市集供应商名称
     */
    @ApiModelProperty(value = "易料市集供应商名称")
    private String offerSupplierName;

    /**
     * 易料市集供应商编码
     */
    @ApiModelProperty(value = "易料市集供应商编码")
    private String offerSupplierCode;

    /**
     * 易料市集商品名
     */
    @ApiModelProperty(value = "易料市集商品名")
    private String goodsName;

    /**
     * 易料市集商品id
     */
    @ApiModelProperty(value = "易料市集商品id")
    private String skuId;

}
