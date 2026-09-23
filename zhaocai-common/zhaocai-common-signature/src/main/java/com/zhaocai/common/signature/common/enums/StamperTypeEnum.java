package com.zhaocai.common.signature.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盖章类型
 *
 * @author chenming
 * @date 2024-08-23
 */
@Getter
@AllArgsConstructor
public enum StamperTypeEnum {

    QYS_COMPANY("COMPANY","公章","SEAL_CORPORATE"),
    QYS_PERSONAL("PERSONAL","个人签名","SEAL_PERSONAL"),
    QYS_LP("LP","法人章","SEAL_CORPORATE"),
    QYS_TIMESTAMP("TIMESTAMP","时间戳","TIMESTAMP"),
    QYS_ACROSS_PAGE("ACROSS_PAGE","骑缝章","ACROSS_PAGE"),

    ;

    /**
     * 类型
     */
    private final String type;

    /**
     * 描述
     */
    private final String name;

    /**
     * 所对应的平台类型
     */
    private String platformType;

    public Boolean equalsType(String type) {
        return this.getType().equals(type);
    }

    /**
     * 是否为骑缝章
     * @param type
     * @return
     */
    public static Boolean isAcrossPage(String type) {
        return QYS_ACROSS_PAGE.getType().equals(type);
    }
}
