package com.zhaocai.business.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 合同台账报表Vo
 */
@Data
public class ContractBaseReportVo {
    /** 组织id(第三方) */
    private String id;

    /** 项目编码 */
    private String minAccountCode;

    /** 部门id */
    private String deptId;

    /** 部门名称 */
    private String deptName;

    /** 部门ids */
    private List<String> deptIds;

    /** 上级id */
    private String parentId;

    /** 类型 */
    private String type;

    /** 唯一id(合同id) */
    private String uniqueId;

    /** 支出业务分类( Z-其他 A-劳务分包 B-专业分包 C-购买材料 D-租赁材料 G-设备租赁（机械）) */
    private String conType;

    /** 支出业务分类( Z-其他 A-劳务分包 B-专业分包 C-购买材料 D-租赁材料 G-设备租赁（机械）) */
    private String conTypeName;

    /** 合同编码 */
    private String conCode;

    /** 合同名称 */
    private String conName;

    /** 合同签订金额（不含税）（元） */
    private BigDecimal ntaxSignAmount;

    /** 合同变更后金额（元）（不含税） */
    private BigDecimal ntaxChangedAmount;

    /** 合同乙方,可配置 */
    private String partbId;

    /** 合同乙方名称 */
    private String partbName;

    /** 乙方现场实际履职负责人 */
    private String partbSiteManager;

    /** 乙方现场实际履职负责人身份证 */
    private String partbSiteManagerIdCard;

    /** 乙方现场实际履职负责人电话 */
    private String partbSiteManagerPhone;

    /** 归属管理组织 */
    private String managementOrgId;

    /** 归属本级租住 */
    private String belongingOrgId;

    /** 项目id */
    private String projectId;

    /** 项目名称 */
    private String minAccountFullName;

    /** 合同笔数 */
    private Integer contractNumber;

    /** 子项 */
    private List<ContractBaseReportVo> Children;

    /** 物料明细-成本科目id */
    private String subjectId;

    /** 物料明细-成本科目编码 */
    private String subjectCode;

    /** 物料明细-成本科目名称 */
    private String subjectName;

    /** 物料明细-成本科目品牌 */
    private String brand;

    /** 物料明细-成本科目型号 */
    private String specs;

    /** 物料明细-计量单位 */
    private String measureUnit;

    /** 物料明细-采购单价 */
    private BigDecimal ntaxPrice;

    /** 物料明细-数量 */
    private BigDecimal quantity;

    /** 物料明细-已结算金额（万元） */
    private BigDecimal settledAmount;

    /** 物料明细-未结算金额（万元） */
    private BigDecimal unsettledAmount;

    /** 物料明细-已付款金额（万元） */
    private BigDecimal paidAmount;

    /** 物料明细-未付款金额（万元） */
    private BigDecimal UnpaidAmount;

    /** 物料明细-备注 */
    private String remark;

    /** 签订时间范围-开始时间 */
    private Date startDate;

    /** 签订时间范围-结束时间 */
    private Date endDate;
}
