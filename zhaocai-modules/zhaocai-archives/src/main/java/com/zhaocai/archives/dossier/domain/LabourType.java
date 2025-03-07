package com.zhaocai.archives.dossier.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhaocai.common.core.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 劳务分类对象 tb_labour_type
 *
 * @author lzq
 * @date 2025-01-06
 */
@TableName(value = "tb_labour_type")
public class LabourType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 父级id */
    @Excel(name = "父级id")
    private Long upId;

    /** 劳务分类名称 */
    @Excel(name = "劳务分类名称")
    private String labourName;

    /** 劳务分类类型 */
    @Excel(name = "劳务分类类型")
    private String labourType;

    /** 劳务分类编码 */
    @Excel(name = "劳务分类编码")
    private String labourCode;

    /** 创建人 id */
    @Excel(name = "创建人 id")
    private Long createId;

    /** 修改人 id */
    @Excel(name = "修改人 id")
    private Long updateId;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;

    /** 是否交易标的物 */
    @Excel(name = "是否交易标的物")
    private String isTransaction;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 层级 */
    @Excel(name = "层级")
    private String labourLevel;

    /** 层级 */
    @Excel(name = "层级")
    private String labourLevelCd;

    /** 状态 */
    @Excel(name = "状态")
    private Long state;

    /** 流程实例 id */
    @Excel(name = "流程实例 id")
    private String wfProcessId;

    /** 流程批次 */
    @Excel(name = "流程批次")
    private String wfBatch;

    /** 关联主表id */
    @Excel(name = "关联主表id")
    private String mainId;

    /** 关联主表id */
    @Excel(name = "同步主表id")
    private String hostId;


    /** 机构编码 */
    @Excel(name = "机构编码")
    private String organCode;

    /** 机构编码 */
    @Excel(name = "是否为主库同步数据")
    private String isMain;

    /**
     * 所属层级
     */
    @TableField(exist = false)
    private String belongingLevel;


    /**
     * 查询类型
     */
    @TableField(exist = false)
    private String queryType;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setUpId(Long upId)
    {
        this.upId = upId;
    }

    public Long getUpId()
    {
        return upId;
    }
    public void setLabourName(String labourName)
    {
        this.labourName = labourName;
    }

    public String getLabourName()
    {
        return labourName;
    }
    public void setLabourType(String labourType)
    {
        this.labourType = labourType;
    }

    public String getLabourType()
    {
        return labourType;
    }
    public void setLabourCode(String labourCode)
    {
        this.labourCode = labourCode;
    }

    public String getLabourCode()
    {
        return labourCode;
    }
    public void setCreateId(Long createId)
    {
        this.createId = createId;
    }

    public Long getCreateId()
    {
        return createId;
    }
    public void setUpdateId(Long updateId)
    {
        this.updateId = updateId;
    }

    public Long getUpdateId()
    {
        return updateId;
    }
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit()
    {
        return unit;
    }
    public void setIsTransaction(String isTransaction)
    {
        this.isTransaction = isTransaction;
    }

    public String getIsTransaction()
    {
        return isTransaction;
    }
    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setState(Long state)
    {
        this.state = state;
    }

    public Long getState()
    {
        return state;
    }
    public void setWfProcessId(String wfProcessId)
    {
        this.wfProcessId = wfProcessId;
    }

    public String getWfProcessId()
    {
        return wfProcessId;
    }
    public void setWfBatch(String wfBatch)
    {
        this.wfBatch = wfBatch;
    }

    public String getWfBatch()
    {
        return wfBatch;
    }
    public void setMainId(String mainId)
    {
        this.mainId = mainId;
    }

    public String getMainId()
    {
        return mainId;
    }
    public void setOrganCode(String organCode)
    {
        this.organCode = organCode;
    }

    public String getOrganCode()
    {
        return organCode;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getLabourLevel() {
        return labourLevel;
    }

    public void setLabourLevel(String labourLevel) {
        this.labourLevel = labourLevel;
    }

    public String getLabourLevelCd() {
        return labourLevelCd;
    }

    public void setLabourLevelCd(String labourLevelCd) {
        this.labourLevelCd = labourLevelCd;
    }

    public String getIsMain() {
        return isMain;
    }

    public void setIsMain(String isMain) {
        this.isMain = isMain;
    }

    public String getBelongingLevel() {
        return belongingLevel;
    }

    public void setBelongingLevel(String belongingLevel) {
        this.belongingLevel = belongingLevel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("upId", getUpId())
            .append("labourName", getLabourName())
            .append("labourType", getLabourType())
            .append("labourCode", getLabourCode())
            .append("createBy", getCreateBy())
            .append("createId", getCreateId())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateId", getUpdateId())
            .append("updateTime", getUpdateTime())
            .append("unit", getUnit())
            .append("isTransaction", getIsTransaction())
            .append("deptId", getDeptId())
            .append("labourLevel", getLabourLevel())
            .append("labourLevelCd", getLabourLevelCd())
            .append("state", getState())
            .append("wfProcessId", getWfProcessId())
            .append("wfBatch", getWfBatch())
            .append("mainId", getMainId())
            .append("isMain", getIsMain())
            .append("hostId", getHostId())
            .append("organCode", getOrganCode())
            .toString();
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
}
