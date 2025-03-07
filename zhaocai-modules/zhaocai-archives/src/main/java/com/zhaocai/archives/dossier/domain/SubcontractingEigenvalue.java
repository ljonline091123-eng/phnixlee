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
 * 专业分包特征值对象 tb_subcontracting_eigenvalue
 *
 * @author lzq
 * @date 2025-01-06
 */
@TableName(value = "tb_subcontracting_eigenvalue")
public class SubcontractingEigenvalue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 特征值名称 */
    @Excel(name = "特征值名称")
    private String eigenvalueName;

    /** 特征值编号 */
    @Excel(name = "特征值编号")
    private String eigenvalueCode;

    /** 专业分包类型id */
    @Excel(name = "专业分包类型id")
    private Long typeId;

    /** 特征项id */
    @Excel(name = "特征项id")
    private Long itemId;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 创建人 id */
    @Excel(name = "创建人 id")
    private Long createId;

    /** 修改人 id */
    @Excel(name = "修改人 id")
    private Long updateId;

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

    /** 是否为主库同步数据 */
    @Excel(name = "是否为主库同步数据")
    private String isMain;

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
    public void setEigenvalueName(String eigenvalueName)
    {
        this.eigenvalueName = eigenvalueName;
    }

    public String getEigenvalueName()
    {
        return eigenvalueName;
    }
    public void setEigenvalueCode(String eigenvalueCode)
    {
        this.eigenvalueCode = eigenvalueCode;
    }

    public String getEigenvalueCode()
    {
        return eigenvalueCode;
    }
    public void setTypeId(Long typeId)
    {
        this.typeId = typeId;
    }

    public Long getTypeId()
    {
        return typeId;
    }
    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getItemId()
    {
        return itemId;
    }
    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
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

    public String getIsMain() {
        return isMain;
    }

    public void setIsMain(String isMain) {
        this.isMain = isMain;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("eigenvalueName", getEigenvalueName())
            .append("eigenvalueCode", getEigenvalueCode())
            .append("typeId", getTypeId())
            .append("itemId", getItemId())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createId", getCreateId())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateId", getUpdateId())
            .append("updateTime", getUpdateTime())
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
