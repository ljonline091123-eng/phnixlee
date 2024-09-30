package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商状态
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class VendorStateVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "首次合作单位名称")
    private String firstCooperationCompanyName;

    @ApiModelProperty(value =  "供应商类别")
    private Integer vendorClass;

    @DictCache(dictBizEnum = DictBizEnum.VENDOR_CLASS,filedName = "vendorClass")
    @ApiModelProperty(value =  "供应商类别")
    private String vendorClassText;

    @ApiModelProperty(value =  "供应商等级")
    private Integer vendorLevel;

    @DictCache(dictBizEnum = DictBizEnum.VENDOR_LEVEL,filedName = "vendorLevel")
    @ApiModelProperty(value =  "供应商等级")
    private String vendorLevelText;

    @ApiModelProperty(value = "是否为黑名单")
    private Integer isBlack;

    @ApiModelProperty(value = "供应商状态")
    private Integer state;

    public String getVendorClassText() {
        if (getIsBlack() == 1) {
            this.vendorClassText = "黑名单";
        } else if (getState() == 1) {
            this.vendorClassText = "待审供应商";
        } else if (getVendorClass() == 1) {
            this.vendorClassText = "合格供应商";
        } else if (getVendorClass() == 2) {
            this.vendorClassText = "战略供应商";
        }
        return this.vendorClassText;
    }
}
