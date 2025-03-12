package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 最小核算项目信息对象 tb_min_project
 *
 * @author WH
 * @date 2024-07-16
 */
@Data
public class MinProjectDetailVO {

    @ApiModelProperty(value = "父项目编码")
    private String parentCode;

    @ApiModelProperty(value = "父项目全称")
    private String parentName;

    @ApiModelProperty(value = "父项目简称")
    private String parentSimpleName;

    @ApiModelProperty(value = "项目编码")
    private String projectCode;

    @ApiModelProperty(value = "项目全称")
    private String minAccountCode;

    @ApiModelProperty(value = "项目全称")
    private String minAccountFullName;

    @ApiModelProperty(value = "项目简称")
    private String minAccountSimpleName;

    @ApiModelProperty(value = "归属项目部")
    private String projectDepartment;

    @ApiModelProperty(value = "项目业态")
    private String prjState;

    @ApiModelProperty(value = "项目业态")
    private String prjStateText;

    @ApiModelProperty(value = "项目状态")
    private String state;

    @ApiModelProperty(value = "项目状态")
    private String stateText;

    @ApiModelProperty(value = "工程类型")
    private String prgType;

    @ApiModelProperty(value = "项目资金来源")
    private String moneySec;

    @ApiModelProperty(value = "项目资金来源")
    private String moneySecText;

    @ApiModelProperty(value = "项目管理模式")
    private String prjManageModel;

    @ApiModelProperty(value = "项目管理模式")
    private String prjManageModelText;

    @ApiModelProperty(value = "承包模式")
    private String contractingModel;

    @ApiModelProperty(value = "承包模式")
    private String contractingModelText;

    @ApiModelProperty(value = "业务分类")
    private String busiType;

    @ApiModelProperty(value = "资质")
    private String ziZhi;

    @ApiModelProperty(value = "资质所属单位")
    private String qualifiedUnit;

    @ApiModelProperty(value = "责任单位")
    private String dutyUnit;

    @ApiModelProperty(value = "construtionUnit")
    private String construtionUnit;

    @ApiModelProperty(value = "项目负责人")
    private String projectLeader;

    @ApiModelProperty(value = "项目负责人电话")
    private String projectLeaderPhone;

    @ApiModelProperty(value = "技术负责人")
    private String technicalDirector;

    @ApiModelProperty(value = "技术负责人电话")
    private String technicalDirectorPhone;

    @ApiModelProperty(value = "承揽方式（招标方式）")
    private String zbType;

    @ApiModelProperty(value = "承揽方式（招标方式）")
    private String zbTypeText;

    @ApiModelProperty(value = "招标控制价")
    private String control;

    @ApiModelProperty(value = "标前成本测算利润率")
    private String bqcbRate;

    @ApiModelProperty(value = "中标价（含税）")
    private String biddingPrice;

    @ApiModelProperty(value = "其中暂列金额（含税）")
    private BigDecimal provisionalSum;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "中标时间")
    private Date winningTime;

    @ApiModelProperty(value = "全费用下浮率")
    private BigDecimal costReductionRate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "立项时间")
    private Date lxDate;

    @ApiModelProperty(value = "总建筑面积(㎡)")
    private String allArea;

    @ApiModelProperty(value = "地下总建筑面积(㎡)")
    private String downArea;

    @ApiModelProperty(value = "地上总建筑面积(㎡)")
    private String upArea;

    @ApiModelProperty(value = "最高层数")
    private String totalFull;

    @ApiModelProperty(value = "最高建筑高度（檐口）(m)")
    private String topEaves;

    @ApiModelProperty(value = "结构类型")
    private String jgType;

    @ApiModelProperty(value = "基础形式")
    private String baseType;

    @ApiModelProperty(value = "国家地区")
    private String country;

    @ApiModelProperty(value = "行政区划")
    private String prjAddr;

    @ApiModelProperty(value = "行政区划-省")
    private String prjAddrProvince;

    @ApiModelProperty(value = "行政区划-市")
    private String prjAddrCity;

    @ApiModelProperty(value = "行政区划-区")
    private String prjAddrRegion;

    @ApiModelProperty(value = "详细地址")
    private String prjAddrInfo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际开工日期")
    private Date actualBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际竣工日期")
    private Date actualEndDate;

    @ApiModelProperty(value = "实际工期")
    private String actualConstructionPeriod;

    @ApiModelProperty(value = "taxpayer")
    private String taxpayer;

    @ApiModelProperty(value = "纳税识别号")
    private String taxpayerNo;

    @ApiModelProperty(value = "计税方式")
    private String tax;

    @ApiModelProperty(value = "归属管理组织")
    private String managementOrgId;

//    @ApiModelProperty(value = "对应机构 id")
//    private Long deptId;

    @ApiModelProperty(value = "归属本级组织")
    private String belongingOrgId;
}
