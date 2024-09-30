package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司联系人
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyContactVO {

    /**
     * 联系人名称
     */
    @ApiModelProperty(value = "联系人名称")
    private String name;

    /**
     * 联系人号码
     */
    @ApiModelProperty(value = "联系人号码")
    private String phone;
}
