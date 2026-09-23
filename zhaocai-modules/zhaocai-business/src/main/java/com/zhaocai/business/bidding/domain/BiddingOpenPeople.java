package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 开标人员信息对象 tb_bidding_open_people
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_open_people")
public class BiddingOpenPeople extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 人员id */
    @ApiModelProperty(value =  "人员id")
    private Long userId;

    /** 人员姓名 */
    @ApiModelProperty(value =  "人员姓名")
    private String userName;

    /** 是否开标（0未开标 1已开标） */
    @ApiModelProperty(value =  "是否开标（0未开标 1已开标）")
    private Integer isOpen;

//    @TableField(exist = false)
    @ApiModelProperty(value =  "第三方待办跳转地址")
    private String redirectUrl;

    @Override
    public String toString() {
        return "{" +
                "noticeId:" + noticeId +
                ", schemeId:" + schemeId +
                ", userId:" + userId +
                ", userName:'" + userName + '\'' +
                ", isOpen:" + isOpen +
                ", id:" + super.getId() +
                ", redirectUrl:" + redirectUrl +
                '}';
    }


}
