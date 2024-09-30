package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设置供应商管理员
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class UpdateContactManagerRequestVO {

    @ApiModelProperty(value = "联系人 id")
    private Long contactId;

    @ApiModelProperty(value = "设置管理员（1:设置、0：取消）")
    private Integer isManager;
}
