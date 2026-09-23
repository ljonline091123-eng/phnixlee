package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 供应商分类
 * @author ssy
 * @date 2024/7/13 15:57
 */
@Getter
@Setter
@TableName(value = "tb_vendor_classify")
public class VendorClassify extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "父id")
    private Long parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;

    @ApiModelProperty(value = "层级")
    private String level;

    private String middleCode;

    private String middleName;

}
