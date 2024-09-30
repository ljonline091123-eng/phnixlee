package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新授权文件请求
 *
 * @author chenming
 * @date 2024-06-26
 */
@Data
public class UpdateAuthorizationFileRequestVO {

    @NotNull(message = "联系人 id 不能为空")
    @ApiModelProperty(value = "联系人 id")
    private Long id;

    @NotBlank(message = "授权书文件名不能为空")
    @ApiModelProperty(value = "授权书文件名")
    private String fileName;

    @NotBlank(message = "授权书 url 不能为空")
    @ApiModelProperty(value = "授权书 url")
    private String fileUrl;
}
