package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 企业分类
 *
 * @author chenming
 * @date 2024-06-20
 */
@Data
public class EnterpriseTypeVO {

    @ApiModelProperty(value = "企业分类 code")
    private String enterpriseTypeCode;

    @ApiModelProperty(value = "企业分类名称")
    private String enterpriseTypeName;

    @ApiModelProperty(value = "企业分类")
    private List<EnterpriseTypeVO> enterpriseTypeList;

    public EnterpriseTypeVO(String enterpriseTypeCode,String enterpriseTypeName) {
        this.enterpriseTypeCode = enterpriseTypeCode;
        this.enterpriseTypeName = enterpriseTypeName;
    }
}
