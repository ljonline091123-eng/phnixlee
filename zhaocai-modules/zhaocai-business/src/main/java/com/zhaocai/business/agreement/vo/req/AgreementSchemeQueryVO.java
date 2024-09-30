package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 签订合同-采购方案VO
 *
 * @author chenming
 * @date 2024-06-20
 */
@Data
public class AgreementSchemeQueryVO extends PageRecive {

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;
}
