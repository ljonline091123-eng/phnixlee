package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 招标公告供应商范围对象 tb_tender_notice_range
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_notice_range")
public class TenderNoticeRange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 供应商id */
    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    /** 供应商名称 */
    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    /** 类型（0设置供应商范围 1推荐供应商） */
    @ApiModelProperty(value =  "类型（0设置供应商范围(公开招标) 1推荐供应商）")
    private Integer type;

}
