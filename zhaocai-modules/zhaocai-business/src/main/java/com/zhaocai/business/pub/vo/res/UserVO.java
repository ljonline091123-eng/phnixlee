package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户vo
 *
 * @author chenming
 * @date 2024-09-14
 */
@Data
public class UserVO {

    @ApiModelProperty(value = "用户 id")
    private Long userId;

    @ApiModelProperty(value = "用户名")
    private String userName;
}
