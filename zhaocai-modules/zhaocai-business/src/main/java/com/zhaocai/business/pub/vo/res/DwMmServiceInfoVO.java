package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DwMmServiceInfoVO {

    @ApiModelProperty(value = "资产分类编码")
    private String serviceClassCode;

    @ApiModelProperty(value = "资产分类名称")
    private String serviceClassName;

    @ApiModelProperty(value = "子集")
    public List<DwMmServiceInfoVO> children;
}
