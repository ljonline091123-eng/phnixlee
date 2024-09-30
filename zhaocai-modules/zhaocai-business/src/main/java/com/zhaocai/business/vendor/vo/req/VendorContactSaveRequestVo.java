package com.zhaocai.business.vendor.vo.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商联系人保存
 *
 * @author lsn
 * @date 2024-08-06
 */
@Data
public class VendorContactSaveRequestVo {

    /** 联系人id */
    @ApiModelProperty(value =  "联系人id")
    private Long id;

    /** 供应商id */
    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    /** 联系人名称 */
    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    /** 联系人身份证 */
    @ApiModelProperty(value = "联系人身份证")
    private String contactIdCard;

    /** 联系人电话 */
    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    /** 联系人邮箱 */
    @ApiModelProperty(value = "联系人邮箱")
    private String contactEmail;

    /** 是否为管理员 */
    @ApiModelProperty(value =  "是否为管理员")
    private Integer isManager;

    /** 是否为法人 */
    @ApiModelProperty(value =  "是否为法人")
    private Integer isLegal;

    /** 状态 */
    @ApiModelProperty(value =  "状态")
    private Integer state;

    /** 是否为主要联系人 */
    @ApiModelProperty(value =  "是否为主要联系人")
    private Integer isMainContact;

    @ApiModelProperty(value = "登录用户id")
    private Long loginUserId;

    @ApiModelProperty(value = "授权书 id")
    private Long certificationId;

    @ApiModelProperty(value = "法人授权书")
    private AttachmentVO attachment;

    @TableField(exist = false)
    private String operateComment;
}
