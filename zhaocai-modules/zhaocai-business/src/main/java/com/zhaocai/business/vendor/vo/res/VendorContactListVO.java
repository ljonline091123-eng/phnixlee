package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 供应商联系人列表
 *
 * @author chenming
 * @date 2024/06/03
 */
@Data
public class VendorContactListVO extends AdviceObject {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "登录 id")
    private Long userId;

    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value =  "注册时间")
    private Date createTime;

    @ApiModelProperty(value =  "是否为管理员")
    private Integer isManager;

    @DictCache(dictBizEnum = DictBizEnum.IS_MANAGER,filedName = "isManager")
    @ApiModelProperty(value =  "是否为管理员-文本")
    private String isManagerText;

    @ApiModelProperty(value =  "账号状态")
    private Integer state;

    @DictCache(dictBizEnum = DictBizEnum.CONTACT_STATE,filedName = "state")
    @ApiModelProperty(value =  "账号状态-文本")
    private String stateText;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期开始时间")
    private Date effectiveBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期结束时间")
    private Date effectiveEndDate;

    @ApiModelProperty(value =  "供应商实例id")
    private String wfProcessId;

}
