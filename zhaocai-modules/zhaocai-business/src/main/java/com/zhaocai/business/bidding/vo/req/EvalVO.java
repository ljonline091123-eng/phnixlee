package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/14 11:49
 */
@Data
@ApiModel(value = "EvalDTO", description = "专家评分DTO")
public class EvalVO implements Serializable {
	private static final long serialVersionUID = -969144728815148701L;

	@ApiModelProperty(value = "采购方案id")
	private Long schemeId;

	@ApiModelProperty(value = "招标公告id")
	private Long noticeId;

	@ApiModelProperty(value = "评标的供应商id")
	private Long vendorId;

	@ApiModelProperty(value = "投标单id")
	private Long biddingInfoId;

	@ApiModelProperty(value = "专家评分项信息")
	private List<List<EvalItemVO>> evalItemDTOSList;

	@ApiModelProperty(value = "评标意见")
	private String advice;

}
