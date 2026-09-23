package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 服务 dm073对象 dw_mm_service_inf
 *
 * @author WH
 * @date 2024-08-29
 */
@Data
@TableName(value = "dw_mm_service_inf")
public class DwMmServiceInf {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 资产分类编码
     */
    private String serviceClassCode;

    /**
     * 资产分类名称
     */
    private String serviceClassName;

    /**
     * 分类级次_扩展
     */
    private String serviceClassLevelExt;

    /**
     * 分类级次
     */
    private String serviceClassLevel;

    /**
     * 分类级次cd
     */
    private String serviceClassLevelCd;

    /**
     * 所属上级资产分类_扩展
     */
    private String belgPreServiceClassExt;

    /**
     * 所属上级资产分类名称
     */
    private String belgPreServiceClassName;

    /**
     * 所属上级资产分类id
     */
    private String belgPreServiceClassId;

    /**
     * 所属上级资产分类编码
     */
    private String belgPreServiceClassCode;

    /**
     * 是否标的物_扩展
     */
    private String isSubjectMatterExt;

    /**
     * 是否标的物
     */
    private String isSubjectMatter;

    /**
     * 是否标的物cd
     */
    private String isSubjectMatterCd;

    /**
     * 来源备注
     */
    private String sourceRemark;

    /**
     * 标准税率(%)
     */
    private String standardTaxRate;

    /**
     * 扩展字段
     */
    private String ext1;

    /**
     * 上报状态
     */
    private String reportStatus;

    /**
     * 上报时间
     */
    private String reportTime;

    /**
     * 内部ID
     */
    private String internalId;

    /**
     * 内部父级ID
     */
    private String internalParentId;

    /**
     * 流程实例ID
     */
    private String instanceId;

    /**
     * 流程审批状态
     */
    private String approvalStatus;

    /**
     * 组织ID
     */
    private String deptId;

    /**
     * 加密存储字段
     */
    private String cryptoField;

    /**
     * 由谁创建
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 备注
     */
    private String remark;
}
