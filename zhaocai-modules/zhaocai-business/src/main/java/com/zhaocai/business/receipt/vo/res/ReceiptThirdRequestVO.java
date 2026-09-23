package com.zhaocai.business.receipt.vo.res;

import com.zhaocai.business.manager.http.dto.req.UnderlyingPlatformBaseDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author CFF
 */
@Data
public class ReceiptThirdRequestVO extends UnderlyingPlatformBaseDTO {

    @NotBlank(message = "id不能为空")
    private String id;

    @NotBlank(message = "供应商状态不能为空")
    private String supplierStatus;
}
