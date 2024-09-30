package com.zhaocai.business.pub.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhaocai.common.core.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 项目明细对象 tb_project_work
 *
 * @author cff
 * @date 2024-09-26
 */
@TableName(value = "tb_project_work")
@Data
public class ProjectWork extends BaseEntity {
    private static final long serialVersionUID = 1L;


    /** 采购订单id */
    @Excel(name = "采购订单id")
    private String thirdId;

    /** 父级id */
    @Excel(name = "父级id")
    private String parntId;

    /** 工程承包范围类型 */
    @Excel(name = "工程承包范围类型")
    private String workType;

    /** 工程承包范围类型选项值 */
    @Excel(name = "工程承包范围类型选项值")
    private String workTypeValue;

    /**  */
    @Excel(name = "")
    private String createDept;

    /** 创建人 id */
    @Excel(name = "创建人 id")
    private Long createId;

    /** 修改人 id */
    @Excel(name = "修改人 id")
    private Long updateId;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;


    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("thirdId", getThirdId())
            .append("parntId", getParntId())
            .append("workType", getWorkType())
            .append("workTypeValue", getWorkTypeValue())
            .append("createDept", getCreateDept())
            .append("createBy", getCreateBy())
            .append("createId", getCreateId())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateId", getUpdateId())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
