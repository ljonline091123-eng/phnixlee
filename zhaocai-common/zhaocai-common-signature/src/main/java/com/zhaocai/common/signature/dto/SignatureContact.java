package com.zhaocai.common.signature.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签章联系人
 *
 * @author chenming
 * @date 2024-08-21
 */
@Data
@NoArgsConstructor
public class SignatureContact {

    /**
     * 签章类型
     * 1：合同发起方
     * 2：合同接收方
     */
    private Integer signatureType;

    /**
     * 签署方 id
     */
    private String signatureId;

    /**
     * 签署方名称
     * 即签署方主体名称
     */
    private String signatureName;

    /**
     * 签署方联系人 id
     */
    private Long contactId;

    /**
     * 联系人名称
     */
    private String contactName;

    /**
     * 联系人联系方式
     */
    private String contactPhone;

    /**
     * 联系类型
     * MOBILE（手机号），EMAIL（邮箱），EMPLOYEEID（员工ID），NUMBER（员工编号），BIZID（用户在对接方系统的唯一标识）
     */
    private String contactType;

}
