package com.zhaocai.business.filez.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.zhaocai.business.common.enums.FileZDataTypeEnum;
import com.zhaocai.business.common.enums.FileZLableEnum;
import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.dto.CovertRequestDTO;
import com.zhaocai.business.filez.dto.UpdateBookmarkRefRequestDTO;
import com.zhaocai.business.filez.dto.WatermarkRequestDTO;
import com.zhaocai.business.filez.service.FileZRequestServiceDispatcher;
import com.zhaocai.business.filez.service.IFileZBusinessService;
import com.zhaocai.business.filez.service.dto.AgreementSetLabelDTO;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.file.FileUtils;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 联想文档业务服务类
 *
 * @author chenming
 * @date 2024-07-29
 */
@Service
public class FileZBusinessServiceImpl implements IFileZBusinessService {

    @Override
    public void agreementSetLabelHandler(AgreementSetLabelDTO agreementSetLabel, long agreementId) {
        UpdateBookmarkRefRequestDTO requestDTO = new UpdateBookmarkRefRequestDTO();
        requestDTO.setFileUrl(agreementSetLabel.getAgreementFileUrl());
        requestDTO.setFileName(getFileName(agreementSetLabel.getAgreementName(),agreementSetLabel.getAgreementFileUrl()));

        List<UpdateBookmarkRefRequestDTO.UpdateBookmarkRefListRequestDTO> requestList = new ArrayList<>();
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.AGREEMENT_CODE,agreementSetLabel.getAgreementCode(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.PARTY_A_NAME,agreementSetLabel.getPartyAName(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.PARTY_B_NAME,agreementSetLabel.getPartyBName(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.OBJECT_NAME,agreementSetLabel.getObjectName(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.TOTAL_AMOUNT_INCL_TAX,agreementSetLabel.getTotalAmountInclTax(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.TOTAL_AMOUNT_CHINESE_INCL_TAX,agreementSetLabel.getTotalAmountChineseInclTax(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.TOTAL_AMOUNT_EXCL_TAX,agreementSetLabel.getTotalAmountExclTax(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.TOTAL_AMOUNT_CHINESE_EXCL_TAX,agreementSetLabel.getTotalAmountChineseExclTax(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.PARTY_B_CONTACT_NAME,agreementSetLabel.getPartyBContactName(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.PARTY_B_CONTACT_PHONE,agreementSetLabel.getPartyBContactPhone(),FileZDataTypeEnum.TEXT));
        requestList.add(createBookmarkRefListRequest(FileZLableEnum.PARTY_B_CONTACT_ID_CARD,agreementSetLabel.getPartyBContactIdCard(),FileZDataTypeEnum.TEXT));

        // 暂时不需要清单列表
        // requestList.add(createBookmarkRefListRequest(FileZLableEnum.MATERIALS_LIST,agreementSetLabel.getMaterialsListFileUrl(),FileZDataTypeEnum.DOC));

        requestDTO.setBookmarkRefList(requestList);

        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.UPDATE_BOOKMARK_REF, FileZTaskBusinessEnum.AGREEMENT_UPDATE_BOOKMARK_REF,agreementId);
        requestContext.setFileZRequest(requestDTO);
        FileZRequestServiceDispatcher.getInstance().exchange(requestContext);
    }

    @Override
    public void applyWatermark(String fileName,String text,String fileUrl,FileZTaskBusinessEnum taskBusiness,long businessId) {
        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.APPLY_WATERMARK,taskBusiness, businessId);
        WatermarkRequestDTO requestDTO = new WatermarkRequestDTO();
        requestDTO.setText(text);
        requestDTO.setFileUrl(fileUrl);
        requestDTO.setFileName(getFileName(fileName,fileUrl));
        requestContext.setFileZRequest(requestDTO);
        FileZRequestServiceDispatcher.getInstance().exchange(requestContext);
    }

    @Override
    public void convertToPdf(String fileUrl, String targetFileName, String watermarkText, FileZTaskBusinessEnum taskBusiness, Long businessId) {
        targetFileName = targetFileName + "-" + DateUtil.format(new Date(),"yyyyMMddHHmmss");
        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.CONVERT,taskBusiness, businessId);
        CovertRequestDTO convertRequestDTO = new CovertRequestDTO(fileUrl,targetFileName,"pdf");
        if (StringUtils.isNotBlank(watermarkText)) {
            convertRequestDTO.setWatermarkText(watermarkText);
        }

        requestContext.setFileZRequest(convertRequestDTO);
        FileZRequestServiceDispatcher.getInstance().exchange(requestContext);
    }

    /**
     * 设置请求体
     * @param labelName
     * @param labelValue
     * @param dataType
     * @return
     */
    private UpdateBookmarkRefRequestDTO.UpdateBookmarkRefListRequestDTO createBookmarkRefListRequest(FileZLableEnum labelName, String labelValue, FileZDataTypeEnum dataType) {
        UpdateBookmarkRefRequestDTO.UpdateBookmarkRefListRequestDTO listRequestDTO = new UpdateBookmarkRefRequestDTO.UpdateBookmarkRefListRequestDTO();
        listRequestDTO.setBookname(labelName.getLabel());
        listRequestDTO.setDataType(dataType.name());
        if (FileZDataTypeEnum.TEXT.name().equals(dataType.name())) {
            // 文本
            listRequestDTO.setDataRef(labelValue);
        } else {
            listRequestDTO.setDataRef(labelValue);
            listRequestDTO.setRefName(FileUtils.getName(labelValue));
        }

        return listRequestDTO;
    }

    /**
     * 构建文件名
     * @param name
     * @param fileUrl
     * @return
     */
    private String getFileName(String name,String fileUrl) {
        String fileName = FileUtil.getName(fileUrl);
        return name + "-" + DateUtil.format(new Date(),"yyyyMMddHHmmss") + "." + FileUtil.getSuffix(fileName);
    }
}
