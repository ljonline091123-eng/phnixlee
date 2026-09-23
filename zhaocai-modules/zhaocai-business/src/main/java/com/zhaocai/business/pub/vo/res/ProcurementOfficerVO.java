package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 采购经办人
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementOfficerVO {

    @ApiModelProperty("经办人 id")
    private Long officerId;

    @ApiModelProperty("经办人名称")
    private String officerName;
}
