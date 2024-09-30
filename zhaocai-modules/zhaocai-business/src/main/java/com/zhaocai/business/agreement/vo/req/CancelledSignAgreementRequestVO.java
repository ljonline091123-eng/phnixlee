package com.zhaocai.business.agreement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 作废签署合同请求参数
 *
 * @author chenming
 * @date 2024-09-20
 */
@Data
public class CancelledSignAgreementRequestVO {

    @NotNull(message = "合同 id 不能为空")
    @ApiModelProperty(value = "合同 id")
    private Long id;

    @NotNull(message = "作废原因不能未空")
    @ApiModelProperty(value = "作废原因")
    private String cancelledReason;
}
