package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 供应商联系人变更对象 tb_vendor_contact_change
 *
 * @author lsn
 * @date 2024-08-08
 */
@Getter
@Setter
@TableName(value = "tb_vendor_contact_change")
public class VendorContactChange extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Long contactId;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private Integer version;

    /**
     * 变更状态
     */
    @ApiModelProperty(value = "变更状态")
    private Integer changeStatus;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Long vendorId;

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
