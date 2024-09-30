package com.zhaocai.common.signature.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签章信息对象 tb_agreement_sign
 *
 * @author chenming
 * @date 2024-08-28
 */
@Data
@TableName(value = "tb_agreement_sign")
public class AgreementSign {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务编码
     */
    @TableField("business_code")
    private String businessCode;

    /**
     * 合同id
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 签署状态
     */
    @TableField("sign_state")
    private Integer signState;

    /**
     * 签订信息
     */
    @TableField("sign_message")
    private String signMessage;

    /**
     * 签发主题 id
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 签章发起主体
     */
    @TableField("tenant_name")
    private String tenantName;

    /**
     * 发起人名称
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * 发起人联系方式
     */
    @TableField("creator_contact")
    private String creatorContact;

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
