package com.zhaocai.business.agreement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 合同附件
 *
 * @author chenming
 * @date 2024-08-02
 */
@Data
@AllArgsConstructor
public class AgreementFileVO {

    @ApiModelProperty(value = "附件 id")
    private Long attachmentId;

    @ApiModelProperty(value = "消息")
    private String message;
}
