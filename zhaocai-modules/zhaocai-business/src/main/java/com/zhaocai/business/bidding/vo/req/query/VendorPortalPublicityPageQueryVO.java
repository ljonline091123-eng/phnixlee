package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/25 14:33
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorPortalPublicityPageQueryVO", description = "中标公示列表数据-分页VO")
public class VendorPortalPublicityPageQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -6870110641916878283L;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "招标公告状态")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "采购计划类别")
    private Integer procurementPlanType;

}
