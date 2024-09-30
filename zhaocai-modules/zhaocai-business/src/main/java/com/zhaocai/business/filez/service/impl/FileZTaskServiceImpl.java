package com.zhaocai.business.filez.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZResponseCodeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.common.enums.FileZTaskStateEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.filez.domain.FileZTask;
import com.zhaocai.business.filez.mapper.FileZTaskMapper;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.FileZResponse;
import com.zhaocai.business.filez.vo.req.FileZCallBackVO;
import com.zhaocai.common.core.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * FileZ文档任务Service业务层处理
 *
 * @author WH
 * @date 2024-07-04
 */
@Slf4j
@Service
public class FileZTaskServiceImpl extends ServiceImpl<FileZTaskMapper, FileZTask> implements IFileZTaskService {

    @Override
    public Long addFileZTask(FileZRequestContext requestContext) {
        FileZTask filezTask = new FileZTask();
        filezTask.setBusinessCode(requestContext.getBusinessCodeEnum().getBusinessCode());
        filezTask.setBusinessId(requestContext.getBusinessId());
        filezTask.setTaskState(FileZTaskStateEnum.IN_EXECUTION.getState());
        filezTask.setRequestTime(new Date());
        filezTask.setRequestType(requestContext.getBusinessCodeEnum().getDesc());

        FileZRequestTypeEnum requestType = requestContext.getRequestTypeEnum();
        if (requestType != null) {
            filezTask.setRequestUrl(EnvironmentUtil.getProperty("file-z.apiPrefix") + requestType.getRequestUrl());
            filezTask.setRequestType(requestType.getRequestDesc());
            filezTask.setRequestTypeCode(requestType.getRequestCode());
        }

        super.save(filezTask);
        return filezTask.getId();
    }

    @Override
    public void setFileZTaskRequestBody(long id, String requestBody) {
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getRequestBody,requestBody)
                .eq(FileZTask::getId,id));
    }

    @Override
    public void setFileZTaskResponse(long id, FileZResponse response) {
        Integer state = FileZResponseCodeEnum.isSuccess(response.getCode()) ? FileZTaskStateEnum.EXECUTE_SUCCESS.getState() : FileZTaskStateEnum.EXECUTE_FAILED.getState();
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getResponseTime,new Date())
                .set(FileZTask::getTaskState,state)
                .set(FileZTask::getResponseCode,response.getCode())
                .set(FileZTask::getResponseBody, JSON.toJSONString(response))
                .set(FileZTask::getTaskId, response.getTaskId())
                .eq(FileZTask::getId,id));
    }

    @Override
    public void setFileZTaskCallBack(long id, FileZCallBackVO callBack) {
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getCallBackTime,new Date())
                .set(FileZTask::getCallBackCode,callBack.getCode())
                .set(FileZTask::getCallBackBody, JacksonUtil.toJsonString(callBack))
                .eq(FileZTask::getId,id));
    }

    @Override
    public void setFileZTaskAttachmentId(long id, long attachmentId) {
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getAttachmentId, attachmentId)
                .eq(FileZTask::getId,id));
    }

    @Override
    public FileZTask getByBusinessAndAttachment(Long attachmentId, FileZTaskBusinessEnum businessEnum) {
        return super.getOne(new LambdaQueryWrapper<FileZTask>()
                .eq(FileZTask::getBusinessCode,businessEnum.getBusinessCode())
                .eq(FileZTask::getAttachmentId,attachmentId));
    }

    @Override
    public void addInitialFileZTask(FileZTaskBusinessEnum businessEnum, long attachmentId) {
        // 保证 attachmentId-businessCode 是唯一的
        FileZTask checkFileZTask = super.getOne(new LambdaQueryWrapper<FileZTask>()
                .eq(FileZTask::getAttachmentId,attachmentId)
                .eq(FileZTask::getBusinessCode,businessEnum.getBusinessCode()));
        if (checkFileZTask != null) {
            throw new BusinessException("该合同附件对应的联想文档任务已存在，请稍后重试");
        }

        FileZTask filezTask = new FileZTask();
        filezTask.setBusinessCode(businessEnum.getBusinessCode());
        filezTask.setAttachmentId(attachmentId);
        filezTask.setTaskState(FileZTaskStateEnum.IN_EXECUTION.getState());
        filezTask.setRequestTime(new Date());

        super.save(filezTask);
    }

    @Override
    public void updateTaskStatusByAttachment(Long attachmentId, FileZTaskBusinessEnum businessEnum) {
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getTaskState,FileZTaskStateEnum.EXECUTE_SUCCESS.getState())
                .eq(FileZTask::getAttachmentId,attachmentId)
                .eq(FileZTask::getBusinessCode,businessEnum.getBusinessCode()));
    }

    @Override
    public void setFileZBusinessId(Long attachmentId, FileZTaskBusinessEnum taskBusinessEnum, Long businessId,String editFlag) {
        super.update(new LambdaUpdateWrapper<FileZTask>()
                .set(FileZTask::getBusinessId,businessId)
                // 文档无更新，则直接更新状态为成功
                .set("0".equals(editFlag),FileZTask::getTaskState,FileZTaskStateEnum.EXECUTE_SUCCESS.getState())
                .eq(FileZTask::getAttachmentId,attachmentId)
                .eq(FileZTask::getBusinessCode,taskBusinessEnum.getBusinessCode()));
    }
}
