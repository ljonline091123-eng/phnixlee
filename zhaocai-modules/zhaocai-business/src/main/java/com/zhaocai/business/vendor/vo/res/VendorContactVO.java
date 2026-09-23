package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 供应商联系人
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorContactVO extends AdviceObject {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(hidden = true)
    private Integer isManager;

    @ApiModelProperty(hidden = true)
    private Integer state;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value =  "创建时间")
    private Date createTime;

    @DictCache(dictBizEnum= DictBizEnum.CONTACT_STATE,filedName = "state")
    @ApiModelProperty(value =  "状态")
    private String stateText;

    @DictCache(dictBizEnum= DictBizEnum.IS_MANAGER,filedName = "isManager")
    @ApiModelProperty(value =  "是否为管理员")
    private String isManagerText;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期开始时间")
    private Date effectiveBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期结束时间")
    private Date effectiveEndDate;
}
