package com.zhaocai.business.filez.service;

import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.service.dto.AgreementSetLabelDTO;

/**
 * 联想文档业务服务接口
 *
 * @author chenming
 * @date 2024-07-29
 */
public interface IFileZBusinessService {

    /**
     * 合同设置标签处理类
     * @param agreementSetLabel
     * @param agreementId
     */
    void agreementSetLabelHandler(AgreementSetLabelDTO agreementSetLabel, long agreementId);

    /**
     * 设置合同水印
     * @param text
     * @param fileUrl
     * @param taskBusiness
     * @param businessId
     */
    void applyWatermark(String fileName,String text, String fileUrl, FileZTaskBusinessEnum taskBusiness, long businessId);

    /**
     * 转换为 pdf
     * @param fileUrl
     * @param targetFileName
     * @param watermarkText
     * @param taskBusiness
     * @param businessId
     */
    void convertToPdf(String fileUrl, String targetFileName, String watermarkText, FileZTaskBusinessEnum taskBusiness, Long businessId);
}
