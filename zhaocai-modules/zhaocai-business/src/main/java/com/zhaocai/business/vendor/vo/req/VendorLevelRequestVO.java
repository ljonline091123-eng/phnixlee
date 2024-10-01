package com.zhaocai.business.vendor.vo.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 供应商级请求
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
public class VendorLevelRequestVO {

    @ApiModelProperty(value =  "供应商 id")
    private Long id;

    @ApiModelProperty(value =  "供应商分类")
    private Integer vendorClass;

    @ApiModelProperty(value =  "供应商等级")
    private Integer vendorLevel;

    @ApiModelProperty(value = "附件")
    private List<AttachmentRequestVO> attachmentList;

    @TableField(exist = false)
    private String operateComment;
}
