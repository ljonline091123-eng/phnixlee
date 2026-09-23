package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/5/28 17:54
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorNoticePageQueryVO", description = "展示招标公告列表数据-分页VO")
public class VendorNoticePageQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -7100511654615698190L;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "招标方式（1公开招标 2其它{邀请招标、询价采购、单一来源}）")
    private Integer schemeType;

    @ApiModelProperty(value =  "招标单位")
    private String unit;

    @ApiModelProperty(value =  "供应商注册审批通过时间")
    private Date registerApprovalTime;

}
