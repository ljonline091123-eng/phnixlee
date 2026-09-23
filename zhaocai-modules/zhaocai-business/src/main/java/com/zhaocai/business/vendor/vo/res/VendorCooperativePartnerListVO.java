package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 供应商合作单位
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class VendorCooperativePartnerListVO {

    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "合作单位")
    private String cooperativePartnerName;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "合同签订日期")
    private Date agreementSignDate;
}
