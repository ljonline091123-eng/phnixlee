package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商合作记录列表查询条件
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class VendorCooperationListQueryVO extends PageRecive {

    @ApiModelProperty(value =  "供应商名称")
    private String enterpriseName;

    @ApiModelProperty(value = "联系人名称")
    private String contactName;

    @ApiModelProperty(value = "联系人电话")
    private String contactPhone;
}
