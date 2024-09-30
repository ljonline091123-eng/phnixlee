package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修改联系人状态
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class UpdateContactStateRequestVO {

    @ApiModelProperty(value = "联系人 id")
    private Long contactId;

    @ApiModelProperty(value = "状态（1:启用、0：禁用）")
    private Integer state;
}
