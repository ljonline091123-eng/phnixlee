package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 评标专家人员信息对象 tb_bidding_evaluat_expert
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_evaluat_expert")
public class BiddingEvaluatExpert extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 专家人员id */
    @ApiModelProperty(value =  "专家人员id")
    private Long expertId;

    /** 专家人员姓名 */
    @ApiModelProperty(value =  "专家人员姓名")
    private String expertName;

    /** 允许评标截止时间（保留） */
    @ApiModelProperty(value =  "允许评标截止时间")
    private Date deadline;

    /** 专家是否完成评标（0未评标 1已评标） */
    @ApiModelProperty(value =  "专家是否完成评标（0未评标 1已评标）")
    private Integer evalStatus;

    /** 专家是否需要评标（0是 1否） */
    @ApiModelProperty(value =  "专家是否需要评标（0是 1否）")
    //字段暂时不用
    private Integer isEval;

    @ApiModelProperty(value =  "专家类别（1技术类 2经济类）")
    private Integer expertType;

    @ApiModelProperty(value =  "是否参加评标（1是 2否）")
    private Integer isJoin;

}
