package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/18 20:27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EvalTaskPageVO", description = "评标任务列表-分页VO")
public class EvalTaskPageVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -7352548649254517406L;

    @ApiModelProperty(value =  "专家人员id")
    private Long expertId;

//    @ApiModelProperty(value =  "专家是否完成评标（0未评标 1已评标）")
//    private Integer evalStatus;

    @ApiModelProperty(value =  "专家是否开启评标（0否-默认 1是）")
    private Integer isEval;

    @ApiModelProperty(value =  "状态")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "已完成状态")
    private Integer doneNoticeStatus;

}
