package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.service.IAgreementFileService;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.common.enums.FileZTaskStateEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.business.filez.domain.FileZTask;
import com.zhaocai.business.filez.service.IFileZBusinessService;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.filez.service.dto.AgreementSetLabelDTO;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 合同联想文档处理
 *
 * @author chenming
 * @date 2024-07-29
 */
@Slf4j
@Service
public class AgreementFileServiceImpl implements IAgreementFileService {

    @Autowired
    private IFileZTaskService fileZTaskService;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private IFileZBusinessService fileZBusinessService;

    @Autowired
    private YOZOfileUtils yozOfileUtils;


    @Override
    public void setAgreementLabel(Agreement agreement, List<AgreementMaterialsList> agreementMaterialsLists, String templateEditFlag) {
        log.info("[创建合同联想文档-{}] - 开始校验创建合同时联想文档任务执行状态",agreement.getId());

        Long attachmentId = checkFileZTask(agreement.getAttachmentId(),agreement.getId(),templateEditFlag);
        log.info("[创建合同联想文档-{}] - 开始设置标签值,使用附件 id:{}",agreement.getId(),attachmentId);

        AgreementSetLabelDTO agreementSetLabel = new AgreementSetLabelDTO();
        agreementSetLabel.setAgreementCode(agreement.getAgreementCode());
        agreementSetLabel.setAgreementName(agreement.getAgreementName());
        agreementSetLabel.setPartyAName(agreement.getPartyAName());
        agreementSetLabel.setPartyBName(agreement.getPartyBName());
        agreementSetLabel.setObjectName(agreement.getBelongAccountingItem());

        BigDecimal totalTaxPrice = BigDecimal.ZERO;
        BigDecimal totalNotTaxPrice = BigDecimal.ZERO;

        agreementSetLabel.setTotalAmountInclTax(NumberUtil.decimalFormat("#,##0.00",totalTaxPrice));
        agreementSetLabel.setTotalAmountExclTax(NumberUtil.decimalFormat("#,##0.00",totalNotTaxPrice));
        agreementSetLabel.setTotalAmountChineseInclTax(Convert.digitToChinese(totalTaxPrice));
        agreementSetLabel.setTotalAmountChineseExclTax(Convert.digitToChinese(totalNotTaxPrice));

//      暂时去掉清单列表
//        log.info("[创建合同联想文档-{}] - 开始设置清单标签值",agreement.getId());
//        String materialsListFileUrl = createMaterialsListFile(agreementMaterialsLists,agreement.getId(),agreement.getAgreementName());
//        agreementSetLabel.setMaterialsListFileUrl(materialsListFileUrl);

        agreementSetLabel.setPartyBContactName(agreement.getPartyBLegalName());
        agreementSetLabel.setPartyBContactIdCard(agreement.getPartyBLegalIdCard());
        agreementSetLabel.setPartyBContactPhone(agreement.getPartyBLegalPhone());

        Attachment attachment = attachmentService.getById(attachmentId);
        agreementSetLabel.setAgreementFileUrl(attachment.getFileUrl());

        log.info("[创建合同联想文档-{}] - 设置清单标签值完成，开始发送请求",agreement.getId());
        fileZBusinessService.agreementSetLabelHandler(agreementSetLabel,agreement.getId());

        log.info("[创建合同联想文档-{}] - 请求发送成功",agreement.getId());
    }

    @Override
    public void applyWatermark(String agreementName,String text, String fileUrl,long agreementId) {
        log.info("[设置合同水印-{}] - 开始发送设置合同水印请求",agreementId);
        fileZBusinessService.applyWatermark(agreementName,text,fileUrl,FileZTaskBusinessEnum.AGREEMENT_APPLY_WATERMARK,agreementId);

        log.info("[设置合同水印-{}] - 设置合同水印请求发送成功",agreementId);
    }

    @Override
    public void agreementConvertToPdf(Long agreementId, String watermarkText, String agreementName, String fileUrl) {
        log.info("[合同转换为 pdf-{}] - 开始发送合同转换为 pdf请求",agreementId);

        //联想文档转pdf
        fileZBusinessService.convertToPdf(fileUrl,agreementName,watermarkText,FileZTaskBusinessEnum.AGREEMENT_CONVERT_TO_PDF,agreementId);

        log.info("[合同转换为 pdf-{}] - 合同转换为 pdf请求发送成功",agreementId);
    }

    /**
     * 合同转换为PDF（永中文档中台），把pdf文件url存入到pdfAttachmentId对应的附件fileUrl中
     *
     * @param pdfAttachmentId
     * @return
     */
    @Override
    public Long agreementConvertToPdfYOZO(Long agreementId, Long pdfAttachmentId, String watermarkText, String fileName, String fileUrl) {
        if (pdfAttachmentId != null) {
            //判断传入的pdfAttachmentId是不是word文件,非word文件不用转pdf，直接返回pdfAttachmentId
            AttachmentVO attachment = attachmentService.getAttachmentById(pdfAttachmentId);
            String OriginalfileName = attachment.getFileName();
            String Suffix = yozOfileUtils.getSuffix(OriginalfileName);
            if (!yozOfileUtils.isWordExtension(Suffix)){
                return pdfAttachmentId;
            }
        }

        //永中文档中台转换pdf,存入到合同的 pdf附件id 对应的fileUrl中
        try {
            //返回存入pdf文件URl的对应附件AttachmentId
            Long attachmentId = attachmentService.ConverToPDFAndUpdateFileUrl(agreementId,AttachmentTypeEnum.AGREEMENT_PDFFILE,pdfAttachmentId,watermarkText,fileName,fileUrl);
            return attachmentId;
        } catch (IOException e) {
            throw new RuntimeException("永中文档中台转换pdf失败"+e.getMessage(), e);
        }

    }

    /**
     * 新建合同清单列表
     * @param agreementMaterialsLists
     * @return
     */
    private String createMaterialsListFile(List<AgreementMaterialsList> agreementMaterialsLists,long agreementId,String agreementName) {
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable();

        // 创建表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("序号");
        headerRow.addNewTableCell().setText("产品编码");
        headerRow.addNewTableCell().setText("产品名称");
        headerRow.addNewTableCell().setText("计量单位");
        headerRow.addNewTableCell().setText("数量");
        headerRow.addNewTableCell().setText("不含税单价（元）");
        headerRow.addNewTableCell().setText("含税单价（元）");

        for(int i = 0 ; i < agreementMaterialsLists.size() ; i++) {
            AgreementMaterialsList materialsList = agreementMaterialsLists.get(i);

            XWPFTableRow dataRow = table.createRow();

            dataRow.getCell(0).setText(String.valueOf(i + 1));
            dataRow.getCell(1).setText(materialsList.getMaterialsCode());
            dataRow.getCell(2).setText(materialsList.getMaterialsName());
            dataRow.getCell(3).setText("-");
            dataRow.getCell(4).setText(materialsList.getSignCount().toString());
            if (materialsList.getSignUnitPriceInclTax() != null) {
                dataRow.getCell(5).setText(NumberUtil.decimalFormat("#,##0.00",materialsList.getSignUnitPriceInclTax()));
            } else {
                dataRow.getCell(5).setText("");
            }

            if (materialsList.getSignUnitPriceExclTax() != null) {
                dataRow.getCell(6).setText(NumberUtil.decimalFormat("#,##0.00",materialsList.getSignUnitPriceExclTax()));
            } else {
                dataRow.getCell(6).setText(null);
            }
        }

        InputStream inputStream = null;
        String fileUrl;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            document.write(baos);
            inputStream = new ByteArrayInputStream(baos.toByteArray());

            String fileName = agreementName + "-清单列表文档.docx";
            fileUrl = sysFileService.uploadFile(inputStream,fileName);
            AttachmentRequestVO requestVO = new AttachmentRequestVO(fileName,fileUrl);
            attachmentService.addAttachment(requestVO, AttachmentTypeEnum.AGREEMENT_MATERIALS_LIST,agreementId);
        } catch (Exception ex) {
            log.error("创建合同清单类 word 失败,cause by :{}",ex.getMessage(),ex);
            throw new BusinessException(ResultCode.FAILURE,"创建合同清单类 word 失败",ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return fileUrl;
    }

    /**
     * 校验任务
     * @param attachmentId
     */
    private Long checkFileZTask(long attachmentId,long agreementId,String templateEditFlag) {
        if ("0".equals(templateEditFlag)) {
            // 为 0 ，标志附件没有做编辑，可直接用
            log.info("[创建合同联想文档-{}] - 合同附件未做变更，可直接使用原附件",agreementId);
            return attachmentId;
        }

        /*
         * 合同附件已编辑，需要等待任务处理完成
         */
        int tryTimes = 1;
        FileZTask agreementCreateTask = null;
        while (tryTimes <= 30) {
            agreementCreateTask = fileZTaskService.getByBusinessAndAttachment(attachmentId, FileZTaskBusinessEnum.AGREEMENT_CREATE);
            if (agreementCreateTask == null) {
                throw new BusinessException("获取联想文件处理任务失败，请联系开发处理");
            }

            if (FileZTaskStateEnum.EXECUTE_FAILED.equalsState(agreementCreateTask.getTaskState())) {
                throw new BusinessException("创建合同的联想文档任务执行失败");
            }

            if (FileZTaskStateEnum.EXECUTE_SUCCESS.equalsState(agreementCreateTask.getTaskState())) {
                log.info("[创建合同联想文档-{}] - 任务执行成功,",agreementId);
                return agreementCreateTask.getAttachmentId();
            }

            log.info("[创建合同联想文档-{}] - 任务执行中.[tryTimes:{}],",agreementId,tryTimes);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tryTimes++;
        }
        log.warn("[创建合同联想文档-{}] - 等待任务已超过最大执行次数了（60 秒），直接使用原文档,attachmentId:{}",agreementId,attachmentId);
        return attachmentId;
    }
}
