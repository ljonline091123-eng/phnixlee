package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/6 14:16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TwiceBidListVO", description = "二次报价列表数据VO")
public class TwiceBidListVO {

    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "招标单位")
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间|发布时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    @ApiModelProperty(value =  "是否二次报价（未二次报价|已二次报价）")
    private String twiceBidStatus;


}
