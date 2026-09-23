package com.zhaocai.business.vendor.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 更新授权文件请求
 *
 * @author chenming
 * @date 2024-06-26
 */
@Data
public class UpdateAuthorizationDateRequestVO {

    @ApiModelProperty(value = "联系人 id")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "开始时间")
    private Date beginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "结束时间")
    private Date endDate;
}
