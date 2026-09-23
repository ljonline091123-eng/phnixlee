package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 资产 dm0731/材料分类对象 dw_mm_asset_inf
 *
 * @author chenming
 * @date 2024-08-28
 */
@Data
@TableName(value = "dw_mm_asset_inf")
public class DwMmAssetInf {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 资产分类编码
     */
    private String assetClassCode;

    /**
     * 资产分类名称
     */
    private String assetClassName;

    /**
     * 分类级次_扩展
     */
    private String assetClassLevelExt;

    /**
     * 分类级次
     */
    private String assetClassLevel;

    /**
     * 分类级次cd
     */
    private String assetClassLevelCd;

    /**
     * 所属上级资产分类_扩展
     */
    private String belgPreAssetClassExt;

    /**
     * 所属上级资产分类名称
     */
    private String belgPreAssetClassName;

    /**
     * 所属上级资产分类id
     */
    private String belgPreAssetClassId;

    /**
     * 所属上级资产分类编码
     */
    private String belgPreAssetClassCode;

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
     * 单位
     */
    private String measureUnit;

    /**
     * 映射资产分类编码
     */
    private String mapAssetClassCode;

    /**
     * 映射资产分类名称
     */
    private String mapAssetClassName;

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
