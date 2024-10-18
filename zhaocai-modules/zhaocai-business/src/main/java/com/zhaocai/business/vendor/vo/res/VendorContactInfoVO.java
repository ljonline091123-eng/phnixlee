package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.vendor.domain.Vendor;
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
public class VendorContactInfoVO extends AdviceObject {
    private static final long serialVersionUID = 1L;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Long vendorId;

    /**
     * 供应商
     */
    @ApiModelProperty(value = "供应商")
    private VendorVO vendorVO;

    /**
     * 联系人名称
     */
    @ApiModelProperty(value = "联系人名称")
    private String contactName;

    /**
     * 联系人身份证
     */
    @ApiModelProperty(value = "联系人身份证")
    private String contactIdCard;

    /**
     * 联系人电话
     */
    @ApiModelProperty(value = "联系人电话")
    private String contactPhone;

    /**
     * 联系人邮箱
     */
    @ApiModelProperty(value = "联系人邮箱")
    private String contactEmail;

    /**
     * 是否为管理员
     */
    @ApiModelProperty(value = "是否为管理员")
    private Integer isManager;

    /**
     * 是否为法人
     */
    @ApiModelProperty(value = "是否为法人")
    private Integer isLegal;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer state;

    /**
     * 签订状态
     */
    @ApiModelProperty(value = "签订状态")
    private Integer signState;

    /**
     * 签章认证失败原因
     */
    @ApiModelProperty(value = "签章认证失败原因")
    private String signAuthFailReason;

    /**
     * 是否为主要联系人
     */
    @ApiModelProperty(value = "是否为主要联系人")
    private Integer isMainContact;

    @ApiModelProperty(value = "登录用户id")
    private Long loginUserId;

    @ApiModelProperty(value = "授权书 id")
    private Long certificationId;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;
}
