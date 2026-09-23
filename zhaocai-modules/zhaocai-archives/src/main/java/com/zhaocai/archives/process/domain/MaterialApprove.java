package com.zhaocai.archives.process.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * 物料审批辅对象 tb_material_approve
 *
 * @author lzq
 * @date 2025-02-12
 */
@TableName(value = "tb_material_approve")
public class MaterialApprove extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 材料
     */
    public static final String CL_TYPE = "0";

    /**
     * 设备
     */
    public static final String SB_TYPE = "1";

    /**
     * 劳务
     */
    public static final String LW_TYPE = "2";

    /**
     * 专业分包
     */
    public static final String ZYFB_TYPE = "3";

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型
     */
    @Excel(name = "类型")
    private String type;

    /**
     * 状态
     */
    @Excel(name = "状态")
    private Long state;

    /**
     * 关联业务id
     */
    @Excel(name = "关联业务id")
    private Long joinId;

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
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;


    /**
     * 流程实例 id
     */
    @Excel(name = "流程实例 id")
    private String wfProcessId;

    @TableField(exist = false)
    private List<MaterialItemVo> materialItemVos;


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setState(Long state) {
        this.state = state;
    }

    public Long getState() {
        return state;
    }

    public void setJoinId(Long joinId) {
        this.joinId = joinId;
    }

    public Long getJoinId() {
        return joinId;
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

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }


    public String getWfProcessId() {
        return wfProcessId;
    }

    public void setWfProcessId(String wfProcessId) {
        this.wfProcessId = wfProcessId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("type", getType())
                .append("state", getState())
                .append("joinId", getJoinId())
                .append("createId", getCreateId())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateId", getUpdateId())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("wfProcessId", getWfProcessId())
                .toString();
    }

    public List<MaterialItemVo> getMaterialItemVos() {
        return materialItemVos;
    }

    public void setMaterialItemVos(List<MaterialItemVo> materialItemVos) {
        this.materialItemVos = materialItemVos;
    }
}
