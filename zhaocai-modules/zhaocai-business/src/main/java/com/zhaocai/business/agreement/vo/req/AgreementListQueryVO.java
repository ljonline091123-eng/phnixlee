package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 合同列表查询请求参数
 *
 * @author chenming
 * @date 2024/06/05
 */
@Data
public class AgreementListQueryVO extends PageRecive {

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "合同状态")
    private String agreementState;

    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "支出业务分类")
    private Integer expenditureBusinessType;
}
