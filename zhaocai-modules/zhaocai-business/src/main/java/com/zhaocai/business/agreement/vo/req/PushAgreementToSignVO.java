package com.zhaocai.business.agreement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 推送至电子签章平台
 *
 * @author chenming
 * @date 2024-09-18
 */
@Data
public class PushAgreementToSignVO {

    @NotNull(message = "合同 id 不能为空")
    @ApiModelProperty(value = "合同 id")
    private Long id;

    @NotNull(message = "甲方签订人不能为空")
    @ApiModelProperty(value = "甲方签订 id")
    private Long partyAUserId;
}
