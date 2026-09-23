package com.zhaocai.business.pub.vo.res;


import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 财务人员
 *
 * @author chenming
 * @date 2024-06-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStaffVO {

    @ApiModelProperty("财务人员 id")
    private String  financeId;

    @ApiModelProperty("财务人员名称")
    private String financeName;
}
