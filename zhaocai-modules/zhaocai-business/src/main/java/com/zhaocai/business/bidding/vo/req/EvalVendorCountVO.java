package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/18 11:10
 */
@Data
@ApiModel(value = "EvalVendorCountVO", description = "查询未评标的供应商数量DTO")
public class EvalVendorCountVO implements Serializable {
	private static final long serialVersionUID = 2657393357577767361L;

	@ApiModelProperty(value = "方案id")
	private Long schemeId;

	@ApiModelProperty(value = "招标公告id")
	private Long noticeId;

	@ApiModelProperty(value = "专家id")
	private Long expertId;

	@ApiModelProperty(value = "评标的供应商id")
	private Long vendorId;

}
