package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首次合作单位 vo
 *
 * @author chenming
 * @date 2024-06-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstCooperationContactVO {

    @ApiModelProperty(value = "首次合作单位联系人名称")
    private String firstCooperationContactName;

    @ApiModelProperty(value = "首次合作单位联系号码")
    private String firstCooperationContactPhone;

    @ApiModelProperty(value = "首次合作单位联系邮箱")
    private String firstCooperationContactEmail;
}
