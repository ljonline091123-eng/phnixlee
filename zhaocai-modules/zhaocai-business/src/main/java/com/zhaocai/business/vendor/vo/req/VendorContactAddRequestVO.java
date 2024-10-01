package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 供应商联系人添加新增
 *
 * @author chenming
 * @date 2024/06/01
 */
@Data
public class VendorContactAddRequestVO {
    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "是否为管理员")
    private Integer isManager;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value = "法人授权书")
    private AttachmentRequestVO attachment;
}
