package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ssy
 * @date 2024/9/12 14:57
 */
@Data
public class ProcurementPlanPushUserVO {

    @ApiModelProperty(value = "用户编号")
    private Long userId;

    @ApiModelProperty(value = "用户昵称")
    private String nickName;

}
