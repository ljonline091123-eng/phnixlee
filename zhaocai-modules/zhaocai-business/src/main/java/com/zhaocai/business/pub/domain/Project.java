package com.zhaocai.business.pub.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhaocai.common.core.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 项目对象 tb_project
 *
 * @author cff
 * @date 2024-09-26
 */
@TableName(value = "tb_project")
@Data
public class Project extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 采购订单id */
    @Excel(name = "采购订单id")
    private String thirdId;

    /** 异常状态 字典key=ABNORMAL_STATUS */
    @Excel(name = "异常状态 字典key=ABNORMAL_STATUS")
    private String abnormalStatus;

    /** 实际竣工日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际竣工日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date actualEndTime;

    /** 实际开工日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际开工日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date actualStartTime;

    /** 实际工期 */
    @Excel(name = "实际工期")
    private BigDecimal actualWork;

    /** 基础形式 字典key=BASIC_FORM */
    @Excel(name = "基础形式 字典key=BASIC_FORM")
    private String basicForm;

    /** 行政区划代码（市） */
    @Excel(name = "行政区划代码", readConverterExp = "市=")
    private String cityCode;

    /** 行政区划代码（市） */
    @Excel(name = "行政区划代码", readConverterExp = "市=")
    private String cityName;

    /** 最高建筑高度檐口(m) */
    @Excel(name = "最高建筑高度檐口(m)")
    private BigDecimal constructionHigh;

    /** 建设单位 */
    @Excel(name = "建设单位")
    private String constructionUnit;

    /** 建设单位Id */
    @Excel(name = "建设单位Id")
    private String constructionUnitId;

    /** 承包模式 字典key=CONTRACT_MODE */
    @Excel(name = "承包模式 字典key=CONTRACT_MODE")
    private String contractMode;

    /** 合同金额 */
    @Excel(name = "合同金额")
    private BigDecimal contractTotalAmount;

    /** 国家和地区代码 */
    @Excel(name = "国家和地区代码")
    private String countryCode;

    /** 国家和地区代码 */
    @Excel(name = "国家和地区代码")
    private String countryName;

    /** 行政区划代码（县） */
    @Excel(name = "行政区划代码", readConverterExp = "县=")
    private String countyCode;

    /** 行政区划代码（县） */
    @Excel(name = "行政区划代码", readConverterExp = "县=")
    private String countyName;

    /** 设计单位 */
    @Excel(name = "设计单位")
    private String designUnit;

    /** 设计单位Id */
    @Excel(name = "设计单位Id")
    private String designUnitId;

    /** 地下总建筑面积(㎡) */
    @Excel(name = "地下总建筑面积(㎡)")
    private BigDecimal downTotalConstructionArea;

    /** 财务负责人id */
    @Excel(name = "财务负责人id")
    private String financeLeaderId;

    /** 财务负责人名称 */
    @Excel(name = "财务负责人名称")
    private String financeLeaderName;

    /** 所属财务组织 */
    @Excel(name = "所属财务组织")
    private String financeOrgName;

    /** 最高层数 */
    @Excel(name = "最高层数")
    private BigDecimal floorNumber;

    /** 全费用下浮率 */
    @Excel(name = "全费用下浮率")
    private BigDecimal fullDropRate;

    /** 归属管理组织id */
    @Excel(name = "归属管理组织id")
    private String manageOrgId;

    /** 归属管理组织名称 */
    @Excel(name = "归属管理组织名称")
    private String manageOrgName;

    /** 归属本级组织id */
    @Excel(name = "归属本级组织id")
    private String orgId;

    /** 归属本级组织名称 */
    @Excel(name = "归属本级组织名称")
    private String orgName;

    /** 父项目编码 */
    @Excel(name = "父项目编码")
    private String parentProjectCode;

    /** 父项目名称 */
    @Excel(name = "父项目名称")
    private String parentProjectName;

    /** 计划开工日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划开工日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date planStartTime;

    /** 计划工期（天） */
    @Excel(name = "计划工期", readConverterExp = "天=")
    private BigDecimal planWork;

    /** 立项时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "立项时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date projectApprovalTime;

    /** 最小核算项目简称 */
    @Excel(name = "最小核算项目简称")
    private String projectAsName;

    /** 最小核算项目编码 */
    @Excel(name = "最小核算项目编码")
    private String projectCode;

    /** 归属项目部id */
    @Excel(name = "归属项目部id")
    private String projectDepartmentId;

    /** 归属项目部名称 */
    @Excel(name = "归属项目部名称")
    private String projectDepartmentName;

    /** 项目业态 */
    @Excel(name = "项目业态")
    private String projectFormat;

    /** 项目资金来源 字典key=PROJECT_FUNDS_SOURCE */
    @Excel(name = "项目资金来源 字典key=PROJECT_FUNDS_SOURCE")
    private String projectFundsSource;

    /** 项目负责人id */
    @Excel(name = "项目负责人id")
    private String projectLeaderId;

    /** 项目负责人电话 */
    @Excel(name = "项目负责人电话")
    private String projectLeaderMobile;

    /** 项目负责人名称 */
    @Excel(name = "项目负责人名称")
    private String projectLeaderName;

    /** 项目管理模式 字段key=PROJECT_MANAGE_MODEL */
    @Excel(name = "项目管理模式 字段key=PROJECT_MANAGE_MODEL")
    private String projectManageModel;

    /** 最小核算项目全称 */
    @Excel(name = "最小核算项目全称")
    private String projectName;

    /** 项目地点 */
    @Excel(name = "项目地点")
    private String projectSite;

    /** 项目状态 字典key=PROJECT_STATUS */
    @Excel(name = "项目状态 字典key=PROJECT_STATUS")
    private String projectStatus;

    /** 工程类型 字典key=PROJECT_TYPE */
    @Excel(name = "工程类型 字典key=PROJECT_TYPE")
    private String projectType;

    /** 行政区划代码（省） */
    @Excel(name = "行政区划代码", readConverterExp = "省=")
    private String provinceCode;

    /** 行政区划代码（省） */
    @Excel(name = "行政区划代码", readConverterExp = "省=")
    private String provinceName;

    /** 其中暂列金额（含税） */
    @Excel(name = "其中暂列金额", readConverterExp = "含=税")
    private BigDecimal provisionalAmount;

    /** 资质所属单位 */
    @Excel(name = "资质所属单位")
    private String qualificationsUnit;

    /** 资质所属单位Id */
    @Excel(name = "资质所属单位Id")
    private String qualificationsUnitId;

    /** 责任单位 */
    @Excel(name = "责任单位")
    private String responsibleUnit;

    /** 责任单位Id */
    @Excel(name = "责任单位Id")
    private String responsibleUnitId;

    /** 纳税识别号 */
    @Excel(name = "纳税识别号")
    private String socialCreditCode;

    /** 结构类型 字典key=STRUCTURE_TYPE */
    @Excel(name = "结构类型 字典key=STRUCTURE_TYPE")
    private String structureType;

    /** 计税方式 字典key=TAX_MODE */
    @Excel(name = "计税方式 字典key=TAX_MODE")
    private String taxMode;

    /** 纳税主体 */
    @Excel(name = "纳税主体")
    private String taxPayers;

    /** 技术负责人id */
    @Excel(name = "技术负责人id")
    private String technologyLeaderId;

    /** 技术负责人电话 */
    @Excel(name = "技术负责人电话")
    private String technologyLeaderMobile;

    /** 技术负责人名称 */
    @Excel(name = "技术负责人名称")
    private String technologyLeaderName;

    /** 总建筑面积(㎡) */
    @Excel(name = "总建筑面积(㎡)")
    private BigDecimal totalConstructionArea;

    /** 地上总建筑面积(㎡) */
    @Excel(name = "地上总建筑面积(㎡)")
    private BigDecimal upTotalConstructionArea;

    /** 中标价（含税） */
    @Excel(name = "中标价", readConverterExp = "含=税")
    private BigDecimal winningBidAmount;

    /** 中标时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "中标时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date winningBidTime;

    /** 创建人 id */
    @Excel(name = "创建人 id")
    private Long createId;

    /** 修改人 id */
    @Excel(name = "修改人 id")
    private Long updateId;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;


    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("thirdId", getThirdId())
            .append("abnormalStatus", getAbnormalStatus())
            .append("actualEndTime", getActualEndTime())
            .append("actualStartTime", getActualStartTime())
            .append("actualWork", getActualWork())
            .append("basicForm", getBasicForm())
            .append("cityCode", getCityCode())
            .append("cityName", getCityName())
            .append("constructionHigh", getConstructionHigh())
            .append("constructionUnit", getConstructionUnit())
            .append("constructionUnitId", getConstructionUnitId())
            .append("contractMode", getContractMode())
            .append("contractTotalAmount", getContractTotalAmount())
            .append("countryCode", getCountryCode())
            .append("countryName", getCountryName())
            .append("countyCode", getCountyCode())
            .append("countyName", getCountyName())
            .append("designUnit", getDesignUnit())
            .append("designUnitId", getDesignUnitId())
            .append("downTotalConstructionArea", getDownTotalConstructionArea())
            .append("financeLeaderId", getFinanceLeaderId())
            .append("financeLeaderName", getFinanceLeaderName())
            .append("financeOrgName", getFinanceOrgName())
            .append("floorNumber", getFloorNumber())
            .append("fullDropRate", getFullDropRate())
            .append("manageOrgId", getManageOrgId())
            .append("manageOrgName", getManageOrgName())
            .append("orgId", getOrgId())
            .append("orgName", getOrgName())
            .append("parentProjectCode", getParentProjectCode())
            .append("parentProjectName", getParentProjectName())
            .append("planStartTime", getPlanStartTime())
            .append("planWork", getPlanWork())
            .append("projectApprovalTime", getProjectApprovalTime())
            .append("projectAsName", getProjectAsName())
            .append("projectCode", getProjectCode())
            .append("projectDepartmentId", getProjectDepartmentId())
            .append("projectDepartmentName", getProjectDepartmentName())
            .append("projectFormat", getProjectFormat())
            .append("projectFundsSource", getProjectFundsSource())
            .append("projectLeaderId", getProjectLeaderId())
            .append("projectLeaderMobile", getProjectLeaderMobile())
            .append("projectLeaderName", getProjectLeaderName())
            .append("projectManageModel", getProjectManageModel())
            .append("projectName", getProjectName())
            .append("projectSite", getProjectSite())
            .append("projectStatus", getProjectStatus())
            .append("projectType", getProjectType())
            .append("provinceCode", getProvinceCode())
            .append("provinceName", getProvinceName())
            .append("provisionalAmount", getProvisionalAmount())
            .append("qualificationsUnit", getQualificationsUnit())
            .append("qualificationsUnitId", getQualificationsUnitId())
            .append("responsibleUnit", getResponsibleUnit())
            .append("responsibleUnitId", getResponsibleUnitId())
            .append("socialCreditCode", getSocialCreditCode())
            .append("structureType", getStructureType())
            .append("taxMode", getTaxMode())
            .append("taxPayers", getTaxPayers())
            .append("technologyLeaderId", getTechnologyLeaderId())
            .append("technologyLeaderMobile", getTechnologyLeaderMobile())
            .append("technologyLeaderName", getTechnologyLeaderName())
            .append("totalConstructionArea", getTotalConstructionArea())
            .append("upTotalConstructionArea", getUpTotalConstructionArea())
            .append("winningBidAmount", getWinningBidAmount())
            .append("winningBidTime", getWinningBidTime())
            .append("createBy", getCreateBy())
            .append("createId", getCreateId())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateId", getUpdateId())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
