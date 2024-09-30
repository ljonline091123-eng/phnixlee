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
public class FirstCooperationVO {

    @ApiModelProperty(value = "首次合作单位编码")
    private String firstCooperationCode;

    @ApiModelProperty(value = "首次合作单位名称")
    private String firstCooperationName;
}
