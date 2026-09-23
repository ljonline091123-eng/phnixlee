package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商联系人列表
 *
 * @author chenming
 * @date 2024/06/03
 */
@Data
public class VendorContactListQueryVO extends PageRecive {
    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    /**
     * 供应商 id
     */
    @ApiModelProperty(hidden = true)
    private Long vendorId;

    @ApiModelProperty(value =  "供应商状态")
    private Integer state;

    @ApiModelProperty(value =  "供应商实例id")
    private String wfProcessId;

    @ApiModelProperty(value =  "主键id")
    private Long id;
}
