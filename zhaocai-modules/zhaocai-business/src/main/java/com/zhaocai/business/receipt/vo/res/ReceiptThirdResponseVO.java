package com.zhaocai.business.receipt.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *  返回
 * @author cff
 */
@Data
public class ReceiptThirdResponseVO {

    @ApiModelProperty(value =  "第三方响应id")
    private String id;
}
