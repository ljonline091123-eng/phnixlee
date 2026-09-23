package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商主要联系人
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorMainContactVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value = "联系人身份证")
    private String contactIdCard;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value = "联系人邮箱")
    private String contactEmail;

    @ApiModelProperty(value =  "是否为法人")
    private Integer isLegal;

    @DictCache(dictBizEnum= DictBizEnum.IS_LEGAL,filedName = "isLegal")
    @ApiModelProperty(value =  "是否为法人-文本")
    private String isLegalText;
}
