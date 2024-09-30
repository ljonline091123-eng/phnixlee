package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 推送第三方待办DTO
 * @author ssy
 * @date 2024/8/5 11:12
 */
@Data
public class PushThirdPartyTodoTaskRequestDTO extends UnderlyingPlatformBaseDTO{

    @ApiModelProperty(value =  "内容")
    private List<PushThirdPartyTodoTaskSonRequestDTO> messageList;


}
