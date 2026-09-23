package com.zhaocai.common.signature.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签章信息-契约锁对象 tb_agreement_sign_qys
 *
 * @author WH
 * @date 2024-09-14
 */
@Data
@TableName(value = "tb_agreement_sign_qys")
public class AgreementSignQys {
    private static final long serialVersionUID = 1L;

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
     * 业务 Id
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 合同签章id
     */
    @TableField("sign_id")
    private Long signId;

    /**
     * 签署步骤
     */
    @TableField("sign_step")
    private String signStep;

    /**
     * 合同文档 id
     */
    @TableField("document_id")
    private String documentId;

    /**
     * 电子签约文件id
     */
    @TableField("contract_id")
    private String contractId;

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
