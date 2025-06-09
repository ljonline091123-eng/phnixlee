package com.zhaocai.business.bidding.vo.req;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/4 14:30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingExpertListVO", description = "评标专家人员集合VO")
public class BiddingExpertListVO implements Serializable {
    private static final long serialVersionUID = -2430109612017998526L;

    @ApiModelProperty(value =  "专家人员id")
    private Long expertId;

    @ApiModelProperty(value =  "专家人员姓名")
    private String expertName;

    @ApiModelProperty(value =  "专家类别（1技术类 2经济类）")
    private String expertType;

    @ApiModelProperty(value =  "第三方待办跳转地址")
    private String redirectUrl;

//    @ApiModelProperty(value =  "是否参加评标（1是 2否）")
//    private Integer isJoin;

}
