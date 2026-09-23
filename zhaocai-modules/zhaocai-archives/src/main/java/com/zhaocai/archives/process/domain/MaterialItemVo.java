package com.zhaocai.archives.process.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 材料特征值主对象 mtr_feature_value
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class MaterialItemVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
//    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /**
     * 设备类型id
     */
    // @Excel(name = "设备类型id")
    private Long typeId;

    /**
     * 设备特征名称
     */
    // @Excel(name = "设备特征名称")
    private String itemName;

    /**
     * 设备特征编号
     */
    // @Excel(name = "设备特征编号")
    private String itemCode;

    /**
     * 部门ID
     */
    // @Excel(name = "部门ID")
    private Long deptId;

    /**
     * 创建人 id
     */
    // @Excel(name = "创建人 id")
    private Long createId;

    /**
     * 修改人 id
     */
    // @Excel(name = "修改人 id")
    private Long updateId;

    /**
     * 是否为主库同步数据
     */
    // @Excel(name = "是否为主库同步数据")
    private String isMain;

    /**
     * 状态
     */
    // @Excel(name = "状态")
    private Long state;

    /**
     * 流程实例 id
     */
    // @Excel(name = "流程实例 id")
    private String wfProcessId;

    /**
     * 流程批次
     */
    // @Excel(name = "流程批次")
    private String wfBatch;

    /**
     * 关联主表id
     */
    // @Excel(name = "关联主表id")
    private String mainId;

    /**
     * 关联主表id
     */
    // @Excel(name = "同步主表id")
    private String hostId;


    /**
     * 机构编码
     */
    // @Excel(name = "机构编码")
    private String organCode;


    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    private String delFlag;

}
