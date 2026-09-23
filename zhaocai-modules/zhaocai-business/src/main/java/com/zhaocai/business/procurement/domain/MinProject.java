package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 最小核算项目信息对象 tb_min_project
 *
 * @author WH
 * @date 2024-07-16
 */
@Data
@TableName(value = "tb_min_project")
public class MinProject extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 父项目编码
     */
    @ApiModelProperty(value = "父项目编码")
    private String parentCode;

    /**
     * 父项目全称
     */
    @ApiModelProperty(value = "父项目全称")
    private String parentName;

    /**
     * 父项目简称
     */
    @ApiModelProperty(value = "父项目简称")
    private String parentSimpleName;

    /**
     * 项目编码
     */
    @ApiModelProperty(value = "项目编码")
    private String projectCode;

    /**
     * 最小核算项目编码
     */
    @ApiModelProperty(value = "最小核算项目编码")
    private String minAccountCode;

    /**
     * 项目全称
     */
    @ApiModelProperty(value = "项目全称")
    private String minAccountFullName;

    /**
     * 项目简称
     */
    @ApiModelProperty(value = "项目简称")
    private String minAccountSimpleName;

    /**
     * 归属项目部
     */
    @ApiModelProperty(value = "归属项目部")
    private String projectDepartment;

    /**
     * 归属项目部id
     */
    @ApiModelProperty(value = "归属项目部id")
    private String projectDepartmentId;

    /**
     * 项目业态（房建、交通、市政、水利、新能源、其他）
     */
    @ApiModelProperty(value = "项目业态（房建、交通、市政、水利、新能源、其他）")
    private String prjState;

    /**
     * 工程类型
     */
    @ApiModelProperty(value = "工程类型")
    private String prgType;

    /**
     * 项目资金来源
     */
    @ApiModelProperty(value = "项目资金来源")
    private String moneySec;

    /**
     * 项目管理模式
     */
    @ApiModelProperty(value = "项目管理模式")
    private String prjManageModel;

    /**
     * 承包模式
     */
    @ApiModelProperty(value = "承包模式")
    private String contractingModel;

    /**
     * 业务分类
     */
    @ApiModelProperty(value = "业务分类")
    private String busiType;

    /**
     * 资质
     */
    @ApiModelProperty(value = "资质")
    private String ziZhi;

    /**
     * 资质所属单位
     */
    @ApiModelProperty(value = "资质所属单位")
    private String qualifiedUnit;

    /**
     * 责任单位
     */
    @ApiModelProperty(value = "责任单位")
    private String dutyUnit;

    /**
     * construtionUnit
     */
    @ApiModelProperty(value = "建设单位")
    private String construtionUnit;

    /**
     * 项目负责人
     */
    @ApiModelProperty(value = "项目负责人")
    private String projectLeader;

    /**
     * 项目负责人电话
     */
    @ApiModelProperty(value = "项目负责人电话")
    private String projectLeaderPhone;

    /**
     * 技术负责人
     */
    @ApiModelProperty(value = "技术负责人")
    private String technicalDirector;

    /**
     * 技术负责人电话
     */
    @ApiModelProperty(value = "技术负责人电话")
    private String technicalDirectorPhone;

    /**
     * 承揽方式（招标方式）
     */
    @ApiModelProperty(value = "承揽方式（招标方式）")
    private String zbType;

    /**
     * 招标控制价
     */
    @ApiModelProperty(value = "招标控制价")
    private String control;

    /**
     * 标前成本测算利润率
     */
    @ApiModelProperty(value = "标前成本测算利润率")
    private String bqcbRate;

    /**
     * 中标价（含税）
     */
    @ApiModelProperty(value = "中标价（含税）")
    private String biddingPrice;

    /**
     * 其中暂列金额（含税）
     */
    @ApiModelProperty(value = "其中暂列金额（含税）")
    private BigDecimal provisionalSum;

    /**
     * 中标时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "中标时间")
    private Date winningTime;

    /**
     * 全费用下浮率
     */
    @ApiModelProperty(value = "全费用下浮率")
    private BigDecimal costReductionRate;

    /**
     * 立项时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "立项时间")
    private Date lxDate;

    /**
     * 总建筑面积(㎡)
     */
    @ApiModelProperty(value = "总建筑面积(㎡)")
    private String allArea;

    /**
     * 地下总建筑面积(㎡)
     */
    @ApiModelProperty(value = "地下总建筑面积(㎡)")
    private String downArea;

    /**
     * 地上总建筑面积(㎡)
     */
    @ApiModelProperty(value = "地上总建筑面积(㎡)")
    private String upArea;

    /**
     * 最高层数
     */
    @ApiModelProperty(value = "最高层数")
    private String totalFull;

    /**
     * 最高建筑高度（檐口）(m)
     */
    @ApiModelProperty(value = "最高建筑高度（檐口）(m)")
    private String topEaves;

    /**
     * 结构类型（字典key-STRUCTURE_TYPE框架结构,剪力墙结构,框架-剪力墙结构,装配式结构,砖木结构,砖混结构,钢结构）
     */
    @ApiModelProperty(value = "结构类型（框架结构,剪力墙结构,框架-剪力墙结构,装配式结构,砖木结构,砖混结构,钢结构）")
    private String jgType;

    /**
     * 基础形式
     */
    @ApiModelProperty(value = "基础形式")
    private String baseType;

    /**
     * 国家地区
     */
    @ApiModelProperty(value = "国家地区")
    private String country;

    /**
     * 行政区划
     */
    @ApiModelProperty(value = "行政区划")
    private String prjAddr;

    /**
     * 行政区划-省
     */
    @ApiModelProperty(value = "行政区划-省")
    private String prjAddrProvince;

    /**
     * 行政区划-市
     */
    @ApiModelProperty(value = "行政区划-市")
    private String prjAddrCity;

    /**
     * 行政区划-区
     */
    @ApiModelProperty(value = "行政区划-区")
    private String prjAddrRegion;

    /**
     * 详细地址
     */
    @ApiModelProperty(value = "详细地址")
    private String prjAddrInfo;

    /**
     * 实际开工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际开工日期")
    private Date actualBeginDate;

    /**
     * 实际竣工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际竣工日期")
    private Date actualEndDate;

    /**
     * 实际工期
     */
    @ApiModelProperty(value = "实际工期")
    private String actualConstructionPeriod;

    /**
     * taxpayer
     */
    @ApiModelProperty(value = "taxpayer")
    private String taxpayer;

    /**
     * 纳税识别号
     */
    @ApiModelProperty(value = "纳税识别号")
    private String taxpayerNo;

    /**
     * 计税方式
     */
    @ApiModelProperty(value = "计税方式")
    private String tax;

    /**
     * 归属管理组织
     */
    @ApiModelProperty(value = "归属管理组织")
    private String managementOrgId;

    /**
     * 归属本级组织
     */
    @ApiModelProperty(value = "归属本级组织")
    private String belongingOrgId;

    /**
     * 项目状态，成控系统判断非手动录入
     */
    @ApiModelProperty(value = "项目状态")
    private String state;

    /**
     * 最小核算项目id
     */
    private String minProjectId;
}
