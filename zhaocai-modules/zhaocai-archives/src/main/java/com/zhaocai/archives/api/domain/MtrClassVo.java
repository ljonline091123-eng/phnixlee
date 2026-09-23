package com.zhaocai.archives.api.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 材料分类主对象 mtr_class
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class MtrClassVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    private String id;

    /**
     * 材料分类编码
     */
    // @Excel(name = "材料分类编码")
    private String mtrClassCode;

    /**
     * 材料分类名称
     */
    // @Excel(name = "材料分类名称")
    private String mtrClassName;

    /**
     * 材料分类下级类型 1特征项 2具体材料
     */
    // @Excel(name = "材料分类下级类型 1特征项 2具体材料")
    private String mtrClassType;

    /**
     * 计量单位
     */
    // @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 是否交易标的物 true是 false不是
     */
    // @Excel(name = "是否交易标的物 true是 false不是")
    private Integer subjectMatter;

    /**
     * 父节点
     */
    // @Excel(name = "父节点")
    private String parentId;

    /**
     * 分类层级
     */
    // @Excel(name = "分类层级")
    private String classLevel;

    /**
     * 分类层级
     */
    // @Excel(name = "分类层级")
    private String classLevelCd;

    /**
     * 创建者id
     */
    // @Excel(name = "创建者id")
    private String createId;

    /**
     * 创建者
     */
    // @Excel(name = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    // @Excel(name = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * $column.columnComment
     */
    private Long valid;

    /**
     * 副库id
     */
    private Long sonId;

    /**
     * 是否为主库同步数据
     */
    private String isMain;

    /**
     * 状态
     */
    private Long state;

    /**
     * 流程实例 id
     */
    private String wfProcessId;

    /**
     * 流程批次
     */
    private String wfBatch;

    /**
     * 关联主表id
     */
    private String mainId;


    /**
     * 主库同步id
     */
    private String hostId;

    /**
     * 机构编码
     */
    private String organCode;

}
