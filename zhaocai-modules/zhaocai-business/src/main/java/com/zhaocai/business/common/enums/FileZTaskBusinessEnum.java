package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 联想文档业务枚举
 *
 * @author chenming
 * @date 2024-07-04
 */
@Getter
@AllArgsConstructor
public enum FileZTaskBusinessEnum {

    // 该枚举应用在新建合同时
    AGREEMENT_CREATE("agreement_create","合同创建"),

    AGREEMENT_APPLY_WATERMARK("agreement_apply_watermark","合同-设置水印"),
    AGREEMENT_APPLY_WATERMARK_DOWNLOAD("agreement_apply_watermark_download","合同-设置水印-文件下载"),

    AGREEMENT_UPDATE_BOOKMARK_REF("agreement_update_bookmark_ref","合同-书签内容替换"),
    AGREEMENT_UPDATE_BOOKMARK_REF_DOWNLOAD("agreement_update_bookmark_ref_download","合同-书签内容替换-文件下载"),

    AGREEMENT_CONVERT_TO_PDF("agreement_convert_to_pdf","合同-转换为 pdf（附带水印）"),
    AGREEMENT_CONVERT_TO_PDF_DOWNLOAD("agreement_convert_to_pdf_download","合同-转换为 pdf（附带水印）-文件下载"),
    ;

    private final String businessCode;

    private final String desc;

    /**
     * 根据 code 获取对应枚举
     * @param businessCode
     * @return
     */
    public static FileZTaskBusinessEnum getByBusinessCode(String businessCode) {
        for (FileZTaskBusinessEnum businessEnum : FileZTaskBusinessEnum.values()) {
            if (businessEnum.getBusinessCode().equals(businessCode)) {
                return businessEnum;
            }
        }
        return null;
    }
}
