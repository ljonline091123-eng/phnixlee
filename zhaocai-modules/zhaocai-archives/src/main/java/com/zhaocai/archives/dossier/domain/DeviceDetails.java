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
 * 设备详情对象 tb_device_details
 *
 * @author lzq
 * @date 2025-01-06
 */
@TableName(value = "tb_device_details")
public class DeviceDetails extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 设备类型id */
    @Excel(name = "设备类型id")
    private Long typeId;

    /** 设备编号 */
    @Excel(name = "设备编号")
    private String deviceCode;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 所属分类 */
    @Excel(name = "所属分类")
    private String typeName;

    /** 规格 */
    @Excel(name = "规格")
    private String deviceSpecifications;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;

    /** 创建人 id */
    @Excel(name = "创建人 id")
    private Long createId;

    /** 修改人 id */
    @Excel(name = "修改人 id")
    private Long updateId;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 是否为主库同步数据 */
    @Excel(name = "是否为主库同步数据")
    private String isMain;

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

    /**
     * 特征项
     */
    @Excel(name = "特征项")
    private String feature;

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
    public void setTypeId(Long typeId)
    {
        this.typeId = typeId;
    }

    public Long getTypeId()
    {
        return typeId;
    }
    public void setDeviceCode(String deviceCode)
    {
        this.deviceCode = deviceCode;
    }

    public String getDeviceCode()
    {
        return deviceCode;
    }
    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getDeviceName()
    {
        return deviceName;
    }
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    public String getTypeName()
    {
        return typeName;
    }
    public void setDeviceSpecifications(String deviceSpecifications)
    {
        this.deviceSpecifications = deviceSpecifications;
    }

    public String getDeviceSpecifications()
    {
        return deviceSpecifications;
    }
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit()
    {
        return unit;
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
    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }
    public void setIsMain(String isMain)
    {
        this.isMain = isMain;
    }

    public String getIsMain()
    {
        return isMain;
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

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("typeId", getTypeId())
            .append("deviceCode", getDeviceCode())
            .append("deviceName", getDeviceName())
            .append("typeName", getTypeName())
            .append("deviceSpecifications", getDeviceSpecifications())
            .append("unit", getUnit())
            .append("createBy", getCreateBy())
            .append("createId", getCreateId())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateId", getUpdateId())
            .append("updateTime", getUpdateTime())
            .append("deptId", getDeptId())
            .append("isMain", getIsMain())
            .append("state", getState())
            .append("wfProcessId", getWfProcessId())
            .append("wfBatch", getWfBatch())
            .append("mainId", getMainId())
            .append("hostId", getHostId())
            .append("organCode", getOrganCode())
            .append("feature", getFeature())
            .toString();
    }


    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
}
