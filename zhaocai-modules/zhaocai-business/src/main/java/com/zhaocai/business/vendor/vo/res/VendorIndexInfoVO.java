package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商首页基本信息
 *
 * @author chenming
 * @date 2024/06/04
 */
@Data
public class VendorIndexInfoVO {

    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value = "联系人名称")
    private String contactName;

    @ApiModelProperty(value = "审核状态")
    private Integer approveState;

    @ApiModelProperty(value = "审核信息,批语")
    private String approveMsg;

    @ApiModelProperty(value = "是否为管理员")
    private Integer isManager;

    @ApiModelProperty(value = "是否为黑名单")
    private Integer isBlack;

    @ApiModelProperty(value = "是否可用")
    private Boolean isAvailable;

    @ApiModelProperty(value = "报错信息")
    private String message;

    @ApiModelProperty(value = "联系人手机")
    private String contactPhone;
}
