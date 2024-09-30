package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.expert.domain.ExpertScore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ExpertScoreVo extends ExpertScore {

    @ApiModelProperty(value =  "专家名称")
    private  String  expertName;




}
