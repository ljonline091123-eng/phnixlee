package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/6 14:14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TwiceBidPageQueryVO", description = "二次报价分页列表查询VO")
public class TwiceBidPageQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -8012551883901456408L;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

}
