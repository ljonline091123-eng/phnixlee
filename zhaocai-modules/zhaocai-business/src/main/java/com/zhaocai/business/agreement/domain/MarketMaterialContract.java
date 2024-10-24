package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 易料采购合同信息对象 tb_market_material_contract
 *
 * @author lsn
 * @date 2024-10-22
 */
@Data
@TableName(value = "tb_market_material_contract")
public class MarketMaterialContract {

    /**
     * 合同主键 id
     */
    @ApiModelProperty(value = "合同主键 id")
    private String id;

    /**
     * 采购计划 id
     */
    @ApiModelProperty(value = "采购计划 id")
    private String planId;

    /**
     * 清单 id
     */
    @ApiModelProperty(value = "清单 id")
    private String requireId;

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
     * 合同编码
     */
    @ApiModelProperty(value = "合同编码")
    private String agreementCode;

    /**
     * 合同名称
     */
    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    /**
     * 乙方（供应商）Id
     */
    @ApiModelProperty(value = "乙方（供应商）Id")
    private String vendorId;

    /**
     * 乙方名称
     */
    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    /**
     * 支出业务分类
     */
    @ApiModelProperty(value = "支出业务分类")
    private String expenditureBusinessType;

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
     * 合同履行地
     */
    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

}
