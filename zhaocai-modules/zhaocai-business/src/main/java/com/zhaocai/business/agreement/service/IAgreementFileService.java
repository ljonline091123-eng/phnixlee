package com.zhaocai.business.agreement.service;


import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;

import java.util.List;

/**
 * 合同文件处理接口
 *
 * @author chenming
 * @date 2024-07-29
 */
public interface IAgreementFileService {

    /**
     * 设置合同标签
     * @param agreement
     * @param agreementMaterialsLists
     * @param templateEditFlag
     */
    void setAgreementLabel(Agreement agreement, List<AgreementMaterialsList> agreementMaterialsLists, String templateEditFlag);


    /**
     * 设置水印
     * @param text
     * @param fileUrl
     * @param agreementId
     */
    void applyWatermark(String agreementName,String text, String fileUrl,long agreementId);

    /**
     * 合同转换为 pdf
     * @param id
     * @param watermarkText
     * @param agreementName
     * @param fileUrl
     */
    void agreementConvertToPdf(Long id, String watermarkText, String agreementName, String fileUrl);

    /**
     * 合同转换为PDF（永中文档中台），把pdf文件url存入到pdfAttachmentId的附件中
     *
     * @param pdfAttachmentId
     * @return
     */
    Long agreementConvertToPdfYOZO(Long agreementId, Long pdfAttachmentId, String watermarkText, String fileName, String fileUrl);
}
