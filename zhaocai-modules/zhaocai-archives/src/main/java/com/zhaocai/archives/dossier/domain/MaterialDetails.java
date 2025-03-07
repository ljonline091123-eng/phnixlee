package com.zhaocai.archives.dossier.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 材料详情对象 tb_material_details
 *
 * @author lzq
 * @date 2025-01-06
 */
@TableName(value = "tb_material_details")
public class MaterialDetails extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 材料类型id
     */
    @Excel(name = "材料类型id")
    private Long typeId;

    /**
     * 材料编号
     */
    @Excel(name = "材料编号")
    private String materialCode;

    /**
     * 材料名称
     */
    @Excel(name = "材料名称")
    private String materialName;

    /**
     * 所属分类
     */
    @Excel(name = "所属分类")
    private String typeName;

    /**
     * 规格
     */
    @Excel(name = "规格")
    private String materialSpecifications;

    /**
     * 单位
     */
    @Excel(name = "单位")
    private String unit;

    /**
     * 创建人 id
     */
    @Excel(name = "创建人 id")
    private Long createId;

    /**
     * 修改人 id
     */
    @Excel(name = "修改人 id")
    private Long updateId;

    /**
     * 部门ID
     */
    @Excel(name = "部门ID")
    private Long deptId;

    /**
     * 是否为主库同步数据
     */
    @Excel(name = "是否为主库同步数据")
    private String isMain;

    /**
     * 状态
     */
    @Excel(name = "状态")
    private Long state;

    /**
     * 流程实例 id
     */
    @Excel(name = "流程实例 id")
    private String wfProcessId;

    /**
     * 流程批次
     */
    @Excel(name = "流程批次")
    private String wfBatch;

    /**
     * 关联主表id
     */
    @Excel(name = "关联主表id")
    private String mainId;

    /**
     * 同步主表id
     */
    @Excel(name = "同步主表id")
    private String hostId;


    /**
     * 机构编码
     */
    @Excel(name = "机构编码")
    private String organCode;


    /**
     * 查询类型
     */
    @TableField(exist = false)
    private String queryType;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setMaterialSpecifications(String materialSpecifications) {
        this.materialSpecifications = materialSpecifications;
    }

    public String getMaterialSpecifications() {
        return materialSpecifications;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public Long getCreateId() {
        return createId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
    }

    public Long getUpdateId() {
        return updateId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setIsMain(String isMain) {
        this.isMain = isMain;
    }

    public String getIsMain() {
        return isMain;
    }

    public void setState(Long state) {
        this.state = state;
    }

    public Long getState() {
        return state;
    }

    public void setWfProcessId(String wfProcessId) {
        this.wfProcessId = wfProcessId;
    }

    public String getWfProcessId() {
        return wfProcessId;
    }

    public void setWfBatch(String wfBatch) {
        this.wfBatch = wfBatch;
    }

    public String getWfBatch() {
        return wfBatch;
    }

    public void setMainId(String mainId) {
        this.mainId = mainId;
    }

    public String getMainId() {
        return mainId;
    }

    public void setOrganCode(String organCode) {
        this.organCode = organCode;
    }

    public String getOrganCode() {
        return organCode;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("typeId", getTypeId())
                .append("materialCode", getMaterialCode())
                .append("materialName", getMaterialName())
                .append("typeName", getTypeName())
                .append("materialSpecifications", getMaterialSpecifications())
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
                .toString();
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
}
