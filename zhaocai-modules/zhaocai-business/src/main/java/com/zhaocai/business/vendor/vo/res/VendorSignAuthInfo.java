package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商签订授权信息
 *
 * @author chenming
 * @date 2024-09-12
 */
@Data
public class VendorSignAuthInfo {

    public VendorSignAuthInfo() {
        this.signAvailable = false;
    }

    public VendorSignAuthInfo(Integer signState,String signFailReason) {
        this.signAvailable = true;
        this.signState = signState;
        this.signFailReason = signFailReason;
    }

    @ApiModelProperty(value = "认证是否可用")
    private Boolean signAvailable;

    /**
     * 认证状态
     * 1 : 需要进行个人认证
     * 2 : 需要进行企业认证
     * 3 : 认证全部通过
     */
    @ApiModelProperty(value = "认证状态")
    private Integer signState;

    @ApiModelProperty(value = "认证失败原因")
    private String signFailReason;
}
