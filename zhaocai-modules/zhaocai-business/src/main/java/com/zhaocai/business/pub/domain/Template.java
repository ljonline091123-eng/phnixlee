package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模板对象 tb_template
 *
 * @author WH
 * @date 2024-06-25
 */
@Data
@TableName(value = "tb_template")
public class Template extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 模板分类
     */
    @ApiModelProperty(value = "模板分类")
    private Integer templateType;

    /**
     * 使用单位编码
     */
    @ApiModelProperty(value = "使用单位编码")
    private String usingUnitNo;

    /**
     * 合同类型（1、劳务分包 2、专业分包 3、购买材料 4、租赁材料 5、租赁机械（设备）6、其他）
     */
    @ApiModelProperty(value = "合同类型")
    private String contractType;

    /**
     * 使用单位名称
     */
    @ApiModelProperty(value = "使用单位名称")
    private String usingUnitName;

    /**
     * 是否允许编辑
     */
    @ApiModelProperty(value = "是否允许编辑")
    private Integer isEditable;

    /**
     * 附件id
     */
    @ApiModelProperty(value = "附件id")
    private Long attachmentId;

}
