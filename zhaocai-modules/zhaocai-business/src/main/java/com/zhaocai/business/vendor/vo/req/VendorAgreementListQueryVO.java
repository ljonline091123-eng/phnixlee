package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商业绩查询
 *
 * @author chenming
 * @date 2024/06/11
 */
@Data
public class VendorAgreementListQueryVO extends PageRecive {

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "项目名称")
    private String belongAccountingItem;

    @ApiModelProperty(value = "签订状态")
    private Integer signState;

    @ApiModelProperty(hidden = true)
    private Long vendorId;

    @ApiModelProperty(hidden = true)
    private Integer state;
}
