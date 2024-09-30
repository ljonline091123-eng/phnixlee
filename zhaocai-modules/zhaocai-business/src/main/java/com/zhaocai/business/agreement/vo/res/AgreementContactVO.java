package com.zhaocai.business.agreement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合同联系人
 *
 * @author chenming
 * @date 2024-06-20
 */
@Data
public class AgreementContactVO {

    @ApiModelProperty(value = "联系人姓名")
    private String contactName;

    @ApiModelProperty(value = "联系人手机")
    private String contactPhone;

    @ApiModelProperty(value = "联系人身份证")
    private String contactIdCard;
}
