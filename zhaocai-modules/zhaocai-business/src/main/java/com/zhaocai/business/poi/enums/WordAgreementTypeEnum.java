package com.zhaocai.business.poi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同类型模板文件 根据不同类型使用不同的模板文件生成数据
 */
@Getter
@AllArgsConstructor
public enum WordAgreementTypeEnum {


    DEFAULT( "默认", "templates/template.docx"),
    DEFAULT_PLAN( "采购计划", "templates/planTemplate.docx"),
    DEFAULT_SCHEME( "采购方案", "templates/schemeTemplate.docx"),
    ;

    private final String desc;
    private final String path;

}
