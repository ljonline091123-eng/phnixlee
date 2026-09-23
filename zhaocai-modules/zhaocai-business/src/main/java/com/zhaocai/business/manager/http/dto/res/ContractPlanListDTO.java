package com.zhaocai.business.manager.http.dto.res;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 合约规划列表 DTO
 *
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class ContractPlanListDTO {

    /**
     * 招标时间
     */
    private String bidDate;

    /**
     * 拟定招标方式
     */
    private String bidMode;

    /**
     * 拟定招标方式名称
     */
    private String bidModeName;

    /**
     * 招标责任单位
     */
    private String bidResponsibleOrg;

    /**
     * 招标责任单位名称
     */
    private String bidResponsibleOrgName;

    /**
     * 合约规划编码
     */
    private String conPlanCode;

    /**
     * 合约规划类型ID
     */
    private String conPlanId;

    /**
     * 合约规划类型名称
     */
    private String conPlanName;

    /**
     *合约规划类型
     */
    private String conPlanType;

    /**
     * 合约规划类型
     */
    private String conPlanTypeName;

    /**
     * id
     */
    private String id;

    /**
     * 累计本次结算金额（含税）
     */
    private BigDecimal settlementAmount;

    /**
     * 剩余金额
     */
    private BigDecimal surplusAmount;

    /**
     * 规划金额（含税）
     */
    private BigDecimal taxAmount;

    /**
     * 已发生规划金额（含税）
     */
    private BigDecimal usedAmount;

    /**
     * 外部数据类型 0其它 1 钢筋采购 2 商品砼采购
     */
    private String externalType;

    /**
     * 品牌
     */
    private String brand;
}
