package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/25 15:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorPortalDataStatQueryVO", description = "门户端数据统计接口VO")
public class VendorPortalDataStatQueryVO implements Serializable {
    private static final long serialVersionUID = 6011807438427798696L;


}
