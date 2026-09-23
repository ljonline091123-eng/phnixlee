package com.zhaocai.business.pub.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务字典
 *
 * @author chenming
 * @date 2024/06/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictListVO {

    @ApiModelProperty(value = "标签")
    private String dictLabel;

    @ApiModelProperty(value = "值")
    private String dictValue;

    @JsonIgnore
    private Integer sort;

    @ApiModelProperty(value = "备注")
    private String remark;
}
