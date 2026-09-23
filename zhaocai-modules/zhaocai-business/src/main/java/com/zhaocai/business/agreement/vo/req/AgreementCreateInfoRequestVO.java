package com.zhaocai.business.agreement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 合同创建请求
 *
 * @author chenming
 * @date 2024/06/13
 */
@Data
public class AgreementCreateInfoRequestVO {

    @NotNull(message = "采购方案id不能为空")
    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    @NotNull(message = "合约拆分id不能为空")
    @ApiModelProperty(value = "合约拆分 id")
    private Long splitId;

    @NotNull(message = "供应商id不能为空")
    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    @NotNull(message = "合同签订清单不能为空")
    @ApiModelProperty(value = "合同签订物料清单信息")
    private List<AgreementMaterialsRequestVO> agreementMaterialsList;
}
