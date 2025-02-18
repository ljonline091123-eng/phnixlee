package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 附件类型枚举
 *
 * @author chenming
 * @date 2024/05/31
 */
@Getter
@AllArgsConstructor
public enum AttachmentTypeEnum {

    /** 附件类型枚举 */

    UPDATE_VENDOR_LEVEL("update_vendor_level","修改供应商等级"),
    VENDOR_CONTACT_AUTHORIZATION("vendor_contact_authorization","供应商联系人授权书"),
    ADD_VENDOR_BLACK("add_vendor_black","供应商移入黑名单"),
    REMOVE_VENDOR_BLACK("remove_vendor_black","供应商移除黑名单"),

    SCHEME_BIDDING("scheme_bidding","采购方案-招标文件"),
    SCHEME_CONTRACT("scheme_contract","采购方案-合同"),

    /** 招投标附件 */
    BIDING_DOCUMENT("biding_document","投标标书附件"),
    BIDING_ABANDON_DOCUMENT("biding_abandon_document","废标附件"),
    BIDING_NOTICE_MSG_DOC("biding_notice_msg_doc","招标公告附件"),
    BIDING_NOTICE_DOC("biding_notice_doc","招标文件附件"),
    BIDING_NOTICE_PDF("biding_notice_pdf","招标文件PDF附件"),
    EVAL_DOCUMENT("eval_document","评标附件"),
    CALIBRATION_DOCUMENT("calibration_document","定标附件"),

    EXPERT_RESUME("expert_resume", "专家工作简历附件"),

    TEMPLATE_AGREEMENT("template_agreement","合同模板"),
    TEMPLATE_BIDDING("template_bidding","招标文件模板"),

    FILE_Z_DOWNLOAD("file-z-download","联想文档下载"),

    AGREEMENT_ORIGINAL("agreement_original","合同原始文件"),
    AGREEMENT_PDFFILE("agreement_pdfFile","合同pdf文件"),
    AGREEMENT_MATERIALS_LIST("agreement_materials_List","合同清单列表"),
    AGREEMENT_OTHER("agreement_other","合同其他文件"),

    ;


    private final String type;

    private final String desc;

    public Boolean equalsType(String type) {
        return this.getType().equals(type);
    }
}
