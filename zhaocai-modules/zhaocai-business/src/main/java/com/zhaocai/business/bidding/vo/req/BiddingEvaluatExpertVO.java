package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/4 14:22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingEvaluatExpertVO", description = "评标专家人员信息VO")
public class BiddingEvaluatExpertVO implements Serializable {
    private static final long serialVersionUID = -5019121885853109743L;

    @ApiModelProperty(value =  "招标公告id")
    @NotNull(message = "招标公告id不能为空", groups = {ValidateGroup.AddGroup.class})
    private Long noticeId;

    @ApiModelProperty(value =  "采购方案id")
    @NotNull(message = "采购方案id不能为空", groups = {ValidateGroup.AddGroup.class})
    private Long schemeId;

//    @ApiModelProperty(value =  "评分方法（0综合评分法/1最低评标价法）")
//    @NotNull(message = "评分方法不能为空", groups = {ValidateGroup.AddGroup.class})
//    private Integer evalWay;

    @ApiModelProperty(value =  "评标专家人员集合")
    private List<BiddingExpertListVO> expertListVO;

    @ApiModelProperty(value =  "评标专家人员集合（不参加评标）")
    private List<BiddingExpertListVO> notJoinExpertListVO;



}
