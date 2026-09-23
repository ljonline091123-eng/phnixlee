package com.zhaocai.business.filez.service.impl;

import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZResponseCodeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.filez.domain.FileZTask;
import com.zhaocai.business.filez.dto.FileZDownloadRequestDTO;
import com.zhaocai.business.filez.dto.FileZDownloadResponseDTO;
import com.zhaocai.business.filez.service.FileZRequestServiceDispatcher;
import com.zhaocai.business.filez.service.IFileZCallBackService;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.vo.req.FileZCallBackVO;
import com.zhaocai.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 联想 FileZ 回调处理实现类
 *
 * @author chenming
 * @date 2024-07-04
 */
@Slf4j
@Service
public class FileZCallBackServiceImpl implements IFileZCallBackService {

    @Autowired
    private IFileZTaskService fileZTaskService;

    @Autowired
    private IAgreementService agreementService;

    @Override
    @Async(value = "businessExecutor")
    public void contentUpdateHandler(FileZCallBackVO callBack, long fileZTaskId) {
        FileZTask fileZTask = getFileZTask(fileZTaskId);

        // 设置回调
        fileZTaskService.setFileZTaskCallBack(fileZTaskId,callBack);

        // 下载文档
        String callBackCode = callBack.getCode();
        String taskId = callBack.getTaskId();
        String contentId = callBack.getDetail().getContentId();
        String fileName = callBack.getDetail().getFilename();
        long attachmentId = 0L;
        if (FileZResponseCodeEnum.isSuccess(callBackCode)) {
            if (StringUtils.isNotBlank(taskId) && StringUtils.isNotBlank(contentId)) {
                // 不为空，下载文件
                log.info("[联想文档内容回调] - fileZTaskId:{}，联想任务处理成功，开始下载文档,taskId:{},contentId:{},fileName:{}",fileZTaskId,taskId,contentId,fileName);
                attachmentId = downloadFile(taskId,contentId,fileName,fileZTask.getBusinessCode(),fileZTask.getBusinessId());
                fileZTaskService.setFileZTaskAttachmentId(fileZTaskId,attachmentId);
            } else {
                log.error("[联想文档内容回调] - fileZTaskId:{}，联想任务处理成功，但 taskId || contentId 为空，无法进行任务下载 ,taskId:{},contentId:{},fileName:{}",fileZTaskId,taskId,contentId,fileName);
                return;
            }
        } else {
            log.error("[联想文档内容回调] - fileZTaskId:{},联想任务处理失败，code:{},detail:{},不做任何处理",fileZTaskId,callBackCode,callBack.getDetail().toString());
            return;
        }


        log.info("[联想文档内容回调] - fileZTaskId:{}，文档下载已完成，attachmentId:{}，开始执行下一步操作",fileZTaskId,attachmentId);
        FileZTaskBusinessEnum taskBusinessEnum = FileZTaskBusinessEnum.getByBusinessCode(fileZTask.getBusinessCode());
        if (taskBusinessEnum != null) {
            switch (taskBusinessEnum) {
                case AGREEMENT_UPDATE_BOOKMARK_REF:
                    log.info("[联想文档内容回调] - fileZTaskId:{}，完成合同标签内容替换",fileZTaskId);
                    agreementService.setAgreementFileLabelFinish(fileZTask.getBusinessId(),attachmentId);
                    break;
                case AGREEMENT_APPLY_WATERMARK:
                    log.info("[联想文档内容回调] - fileZTaskId:{}，完成合同文件设置水印任务",fileZTaskId);
                    agreementService.agreementWatermarkFinish(fileZTask.getBusinessId(),attachmentId);
                    break;
                case AGREEMENT_CONVERT_TO_PDF:
                    log.info("[联想文档内容回调] - fileZTaskId:{}，完成合同文件转换为 PDF 任务",fileZTaskId);
                    agreementService.coverToPdfFinish(fileZTask.getBusinessId(),attachmentId);
                    break;
                default:
                    log.info("[联想文档内容回调] - fileZTaskId:{},无下一步操作，回调处理完成",fileZTaskId);
                    break;
            }
        }
    }

    /**
     * 获取联想文档任务
     * @param fileZTaskId
     * @return
     */
    private FileZTask getFileZTask(long fileZTaskId) {
        FileZTask fileZTask = null;
        // 由于外部事务问题，会导致回调的时候，联想文档还没有来到，这里进行循环等待
        for (int i = 1 ; i <= 10 ; i++) {
            log.info("[联想文档内容回调] - fileZTaskId:{}，开始第[{}]次获取文档处理任务...",fileZTaskId,i);
            fileZTask = fileZTaskService.getById(fileZTaskId);
            if (fileZTask != null) {
                log.info("[联想文档内容回调] - fileZTaskId:{}，文档处理任务第[{}]次获取成功...",fileZTaskId,i);
                break;
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (fileZTask == null) {
            throw new BusinessException("获取联想任务[FileZTask:"+fileZTaskId+"]失败...");
        }
        return fileZTask;
    }

    /**
     * 下载文件
     * @param taskId
     * @param contentId
     * @return
     */
    private long downloadFile(String taskId, String contentId,String fileName, String businessCode, Long businessId) {
        FileZTaskBusinessEnum businessEnum = FileZTaskBusinessEnum.getByBusinessCode(businessCode + "_download");
        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.DOWNLOAD, businessEnum, businessId);
        FileZDownloadRequestDTO requestDTO = new FileZDownloadRequestDTO(taskId,contentId,fileName);
        requestContext.setFileZRequest(requestDTO);

        FileZRequestServiceDispatcher.getInstance().exchange(requestContext);

        FileZDownloadResponseDTO responseDTO = (FileZDownloadResponseDTO) requestContext.getFileZResponse();
        return responseDTO.getAttachmentId();
    }


}
