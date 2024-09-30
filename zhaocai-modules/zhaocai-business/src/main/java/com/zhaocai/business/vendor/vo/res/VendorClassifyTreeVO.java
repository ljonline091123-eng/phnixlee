package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/7/13 16:11
 */
@Data
public class VendorClassifyTreeVO {

    public VendorClassifyTreeVO(Long id, Long parentId, String name, String code, String level) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.code = code;
        this.level = level;
    }

    @ApiModelProperty(value = "主键id")
    private Long id;

    @ApiModelProperty(value = "父id")
    private Long parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;

    @ApiModelProperty(value = "层级")
    private String level;

    @ApiModelProperty(value = "子节点")
    List<VendorClassifyTreeVO> children;


}
