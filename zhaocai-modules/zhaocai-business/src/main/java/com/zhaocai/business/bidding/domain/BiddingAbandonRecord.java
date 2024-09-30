package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 投标单废标记录对象 tb_bidding_abandon_record
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_abandon_record")
public class BiddingAbandonRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 招标项目id */
    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 招标项目名称 */
    @ApiModelProperty(value =  "招标项目名称")
    private String projectName;

    /** 废标原因 */
    @ApiModelProperty(value =  "废标原因")
    private String reason;

    /** 废标附件id */
    @ApiModelProperty(value =  "废标附件id")
    private Long attachId;

    @ApiModelProperty(value =  "投标单id集合")
    private String biddingInfoIds;
}
