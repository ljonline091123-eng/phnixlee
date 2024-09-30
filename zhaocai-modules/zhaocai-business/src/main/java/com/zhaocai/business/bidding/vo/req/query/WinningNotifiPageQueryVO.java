package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/20 17:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "WinningNotifiPageQueryVO", description = "中标通知分页列表查询VO")
public class WinningNotifiPageQueryVO  extends PageRecive implements Serializable {
    private static final long serialVersionUID = 9014252163590071591L;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "招标公告状态")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;


}
