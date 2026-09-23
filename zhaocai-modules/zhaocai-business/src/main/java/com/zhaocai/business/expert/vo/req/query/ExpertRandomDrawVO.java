package com.zhaocai.business.expert.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/31 15:15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ExpertRandomDrawVO", description = "专家随机抽取参数VO")
public class ExpertRandomDrawVO implements Serializable {
    private static final long serialVersionUID = -7942044549718779868L;

    @ApiModelProperty(value =  "抽取经济类专家人数")
    private Integer econExpertNum;

    @ApiModelProperty(value =  "抽取技术类专家人数")
    private Integer techExpertNum;

}
