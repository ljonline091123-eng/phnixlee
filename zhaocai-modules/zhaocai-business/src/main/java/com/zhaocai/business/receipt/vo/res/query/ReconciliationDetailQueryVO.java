package com.zhaocai.business.receipt.vo.res.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author zhangxu
 * @date 2024/9/11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReconciliationDetailQueryVO", description = "材料对账单详情查询VO")
public class ReconciliationDetailQueryVO extends PageRecive implements Serializable {
    public static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "对账单编码")
    private String reconciliationCode;

    /**
     * 模板类型切换
     */
    @ApiModelProperty(value = "模板类型切换")
    private String switchTemplateType;
}
