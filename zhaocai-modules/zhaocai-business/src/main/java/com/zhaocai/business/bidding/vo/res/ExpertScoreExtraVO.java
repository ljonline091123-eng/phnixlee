package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.expert.domain.ExpertScore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author ssy
 * @date 2024/7/4 15:02
 */
@Data
@ApiModel(value = "ExpertScoreExtraVO", description = "专家额外信息VO")
public class ExpertScoreExtraVO extends ExpertScore {
    private static final long serialVersionUID = 2355975960194300989L;

    @ApiModelProperty(value =  "专家类别（1技术类 2经济类）")
    private Integer expertType;

}
