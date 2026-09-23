package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商合作
 *
 * @author chenming
 * @date 2024-06-29
 */
@Data
public class VendorCooperationAgreementListQueryVO {

   @ApiModelProperty(value = "供应商 id")
   private Long vendorId;

   @ApiModelProperty(value = "业务支出类型")
   private Integer expenditureBusinessType;
}
