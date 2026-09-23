package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.business.procurement.domain.MaterialsList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 易料市集推送请求参数
 *
 * @author lsn
 * @Date 2024/10/22
 */
@Data
@ApiModel("易料市集推送请求参数")
public class MaterialProcurementPushRequestVO {

    /**
     * 计划id
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 项目编号
     */
    @NotBlank(message = "合约规划的项目编号不能为空")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 物料清单
     */
    @ApiModelProperty(value = "物料清单")
    private List<MaterialsList> materialsLists;
}
