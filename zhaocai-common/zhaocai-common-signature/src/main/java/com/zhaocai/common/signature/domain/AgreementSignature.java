package com.zhaocai.common.signature.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签章信息对象 tb_agreement_signature
 *
 * @author chenming
 * @date 2024-08-28
 */
@Data
@TableName(value = "tb_agreement_signature")
public class AgreementSignature {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 合同签章id
     */
    @TableField("sign_id")
    private Long signId;

    /**
     * 签署方类型
     */
    @TableField("signature_type")
    private Integer signatureType;

    /**
     * 签署方 id
     */
    @TableField("signature_id")
    private String signatureId;

    /**
     * 签署方名称
     */
    @TableField("signature_name")
    private String signatureName;

    /**
     * 签署方联系人 id
     */
    @TableField("signature_contact_id")
    private Long signatureContactId;

    /**
     * 签署方联系人名称
     */
    @TableField("signature_contact_name")
    private String signatureContactName;

    /**
     * 签署方联系人联系方式
     */
    @TableField("signature_contact_phone")
    private String signatureContactPhone;

    /**
     * 签署状态
     */
    @TableField("sign_state")
    private Integer signState;

    /**
     * 签约回调信息
     */
    @TableField("sign_callback_message")
    private String signCallbackMessage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField("update_time")
    private Date updateTime;
}
