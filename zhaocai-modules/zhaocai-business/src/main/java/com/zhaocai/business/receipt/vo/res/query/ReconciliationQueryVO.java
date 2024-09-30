package com.zhaocai.business.receipt.vo.res.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author zhangxu
 * @date 2024/9/10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReconciliationQueryVO", description = "材料对账单列表查询VO")
public class ReconciliationQueryVO extends PageRecive implements Serializable {
    public static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "对账单编码")
    private String reconciliationCode;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    @ApiModelProperty(value = "供应商id")
    private String vendorId;

    /**
     * 模板类型切换
     */
    @ApiModelProperty(value = "模板类型切换")
    private String switchTemplateType;
}
