package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 易料采购合同查询VO
 *
 * @author lsn
 * @date 2024-10-22
 */
@Data
public class MarketMaterialContractQueryVO extends PageRecive {

    @ApiModelProperty(value = "合同主键 id")
    private String id;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String belongAccountingItemCode;
}
