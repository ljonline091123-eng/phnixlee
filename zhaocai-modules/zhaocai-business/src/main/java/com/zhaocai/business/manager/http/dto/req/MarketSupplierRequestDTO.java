package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author xb
 * @date 2025/2/12 15:42
 */
@Data
public class MarketSupplierRequestDTO extends UnderlyingPlatformBaseDTO {

    @ApiModelProperty(value = "供应商名称")
    private String name;

    @ApiModelProperty(value = "统一信用代码")
    private String unifiedSocialCreditCode;

    @ApiModelProperty(value = "营业执照附件")
    private String businessLicense;

    @ApiModelProperty(value = "所在省份")
    private String provinceName;

    @ApiModelProperty(value = "所在城市")
    private String cityName;

    @ApiModelProperty(value = "所在区县")
    private String districtName;
    @ApiModelProperty(value = "详细地址")
    private String address;
    @ApiModelProperty(value = "经营范围")
    private String businessScope;
    @ApiModelProperty(value = "联系人")
    private String contactName;
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;


}
