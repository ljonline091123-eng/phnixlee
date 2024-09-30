package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 企业资质类型
 *
 * @author chenming
 * @date 2024/05/30
 */
@Getter
@AllArgsConstructor
public enum CertificationTypeEnum {

    BUSINESS_LICENSE("BUSINESS_LICENSE","营业执照"),
    INTEGRITY("INTEGRITY","诚信合规材料"),
    LEGAL_AUTHORIZATION("LEGAL_AUTHORIZATION","法人授权书"),
    RELEVANT_CERTIFICATION("RELEVANT_CERTIFICATION","相关资质")

    ;

    private final String type;

    private final String desc;

    public Boolean equalsType(String type) {
        return this.type.equals(type);
    }
}
