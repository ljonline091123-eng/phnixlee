package com.zhaocai.business.manager.http.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 最小核算项目详细信息响应
 *
 * @author chenming
 * @date 2024-07-14
 */
@Data
public class MinProjectDetailResponseDTO {

    /**
     * 父项目编码
     */
    private String parentCode;

    /**
     * 父项目全称
     */
    private String parentName;

    /**
     * 父项目简称
     */
    private String parentSimpleName;

    /**
     * 项目编码
     */
    private String projectCode;

    /**
     * 最小核算项目编号
     */
    private String minAccountCode;

    /**
     * 项目全称
     */
    private String minAccountFullName;

    /**
     * 项目简称
     */
    private String minAccountSimpleName;

    /**
     * 归属项目部
     */
    private String projectDepartment;

    /**
     * 归属项目部id
     */
    private String projectDepartmentId;

    /**
     * 项目业态
     */
    private String prjState;

    /**
     * 工程类型
     */
    private String prgType;

    /**
     * 项目资金来源
     */
    private String moneySec;

    /**
     * 项目管理模式
     */
    private String prjManageModel;

    /**
     * 承包模式
     */
    private String contractingModel;

    /**
     * 业务分类
     */
    private String busiType;

    /**
     * 资质
     */
    private String ziZhi;

    /**
     * 资质所属单位
     */
    private String qualifiedUnit;

    /**
     * 责任单位
     */
    private String dutyUnit;

    /**
     * construtionUnit
     */
    private String construtionUnit;

    /**
     * 项目负责人
     */
    private String projectLeader;

    /**
     * 项目负责人电话
     */
    private String projectLeaderPhone;

    /**
     * 技术负责人
     */
    private String technicalDirector;

    /**
     * 技术负责人电话
     */
    private String technicalDirectorPhone;

    /**
     * 承揽方式（招标方式）
     */
    private String zbType;

    /**
     * 招标控制价
     */
    private String control;

    /**
     * 标前成本测算利润率
     */
    private String bqcbRate;

    /**
     * 中标价（含税）
     */
    private String biddingPrice;

    /**
     * 其中暂列金额（含税）
     */
    private BigDecimal provisionalSum;

    /**
     * 中标时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date winningTime;

    /**
     * 全费用下浮率
     */
    private BigDecimal costReductionRate;

    /**
     * 立项时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date lxDate;

    /**
     * 总建筑面积(㎡)
     */
    private String allArea;

    /**
     * 地下总建筑面积(㎡)
     */
    private String downArea;

    /**
     * 地上总建筑面积(㎡)
     */
    private String upArea;

    /**
     * 最高层数
     */
    private String totalFull;

    /**
     * 最高建筑高度（檐口）(m)
     */
    private String topEaves;

    /**
     * 结构类型
     */
    private String jgType;

    /**
     * 基础形式
     */
    private String baseType;

    /**
     * 国家地区
     */
    private String country;

    /**
     * 行政区划
     */
    private String prjAddr;

    /**
     * 详细地址
     */
    private String prjAddrInfo;

    /**
     * 实际开工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualBeginDate;

    /**
     * 实际竣工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualEndDate;

    /**
     * 实际工期
     */
    private String actualConstructionPeriod;

    /**
     * taxpayer
     */
    private String taxpayer;

    /**
     * 纳税识别号
     */
    private String taxpayerNo;

    /**
     * 计税方式
     */
    private String tax;

    /**
     * 归属管理组织
     */
    private String managementOrgId;

    /**
     * 归属本级组织
     */
    private String belongingOrgId;

    /**
     * 项目状态，成控系统判断非手动录入
     */
    private String state;

    /**
     * 最小核算项目id
     */
    private String id;

}
