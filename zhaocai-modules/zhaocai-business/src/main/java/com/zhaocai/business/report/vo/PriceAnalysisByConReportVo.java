package com.zhaocai.business.report.vo;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PriceAnalysisByConReportVo {

    /** 组织id(第三方) */
    private String id;

    /** 清单id（唯一id） */
    private String uniqueId;

    /** 合同id */
    private String conId;

    /** 物料/清单编码 */
    @Excel(name = "物资编码")
    private String subjectDtlCode;

    /** 物料/清单名称 */
    @Excel(name = "物资名称")
    private String subjectDtlName;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String specs;

    /** 单位 */
    @Excel(name = "单位")
    private String measureUnit;

    /** 品牌 */
    @Excel(name = "品牌")
    private String brand;

    /** 租赁方式 */
    @Excel(name = "租赁方式")
    private String rentMode;

    /** 计租单位 */
    @Excel(name = "计租单位")
    private String rentUnit;

    /** 租赁时间 */
    @Excel(name = "租赁时间")
    private String rentTime;

    /** 计量规则 */
    @Excel(name = "计量规则")
    private String metrologicalRules;

    /** 工作内容 */
    @Excel(name = "工作内容")
    private String basicJob;

    /** 工程量 */
    @Excel(name = "工程量")
    private BigDecimal number;

    /** 数量 */
    @Excel(name = "总量")
    private BigDecimal quantity;

    /** 单价(不含税) */
    @Excel(name = "总价(不含税)")
    private BigDecimal ntaxPrice;

    /** 最新单价(不含税) */
    @Excel(name = "最新单价(不含税)")
    private BigDecimal lastPrice;

    /** 最低单价(不含税) */
    @Excel(name = "最低单价(不含税)")
    private BigDecimal minPrice;

    /** 最高单价(不含税) */
    @Excel(name = "最高单价(不含税)")
    private BigDecimal maxPrice;

    /** 平均单价(不含税) */
    @Excel(name = "平均单价(不含税)")
    private BigDecimal avgPrice;

    /** 部门名称 */
    @Excel(name = "组织机构")
    private String deptName;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String minAccountFullName;

    /** 合同名称 */
    @Excel(name = "合同名称")
    private String conName;

    /** 合同签订时间 */
    @Excel(name = "合同签订时间")
    private Date signDate;

    /** 合同签订区域 */
    @Excel(name = "合同签订区域")
    private String area;

    /** 成交单价(不含税) */
    @Excel(name = "成交单价(不含税)")
    private BigDecimal initialPrice;

    /** 清单创建时间 */
    private Date createTime;

    /** 唯一id(合同id) */
    private String contractUniqueId;

    /** 合同编码 */
    private String conCode;

    /** 项目id */
    private String projectId;

    /** 项目编码 */
    private String minAccountCode;

    /** 归属本级组织 */
    private String belongingOrgId;

    /** 归属项目部 */
    private String projectDepartmentId;

    /** 部门id */
    private String deptId;

    /** 部门ids */
    private List<String> deptIds;

    /** 上级id */
    private String parentId;

    /** 支出业务分类( Z-其他 A-劳务分包 B-专业分包 C-购买材料 D-租赁材料 G-设备租赁（机械）) */
    private String conType;

    /** 子项 */
    private List<PriceAnalysisByConReportVo> Children;

    /** 签订时间范围-开始时间 */
    private Date startDate;

    /** 签订时间范围-结束时间 */
    private Date endDate;

}
