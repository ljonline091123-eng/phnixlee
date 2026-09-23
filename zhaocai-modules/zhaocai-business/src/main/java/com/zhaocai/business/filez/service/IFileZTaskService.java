package com.zhaocai.business.filez.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.domain.FileZTask;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.FileZResponse;
import com.zhaocai.business.filez.vo.req.FileZCallBackVO;

/**
 * FileZ文档任务Service接口
 *
 * @author WH
 * @date 2024-07-04
 */
public interface IFileZTaskService  extends IService<FileZTask> {

    /**
     * 新增文档任务
     * @param requestContext
     * @return
     */
    Long addFileZTask(FileZRequestContext requestContext);

    /**
     * 设置文档任务请求参数
     * @param id
     * @param requestBody
     */
    void setFileZTaskRequestBody(long id, String requestBody);

    /**
     * 设置文档任务响应结果
     * @param id
     * @param response
     */
    void setFileZTaskResponse(long id, FileZResponse response);

    /**
     * 设置文档任务回调信息
     * @param id
     * @param callBack
     */
    void setFileZTaskCallBack(long id, FileZCallBackVO callBack);

    /**
     * 设置文档任务附件信息
     * @param id
     * @param attachmentId
     */
    void setFileZTaskAttachmentId(long id, long attachmentId);

    /**
     * 根据附件和业务类型获取任务
     * @param attachmentId
     * @return
     */
    FileZTask getByBusinessAndAttachment(Long attachmentId, FileZTaskBusinessEnum businessEnum);

    /**
     * 新增最初始的任务<br/>
     * 由于联想文档的请求都是异步操作，所以在处理业务时（如合同），需要知道上一个任务是否已完成，否则操作的文档就是一个中途的文档，会产生错误<br/>
     * 如果使用该方法则需要保证 business_code + attachmentId 唯一
     * @param businessEnum
     * @param attachmentId
     */
    void addInitialFileZTask(FileZTaskBusinessEnum businessEnum, long attachmentId);

    /**
     * 更新任务状态
     * @param attachmentId
     * @param businessEnum
     */
    void updateTaskStatusByAttachment(Long attachmentId, FileZTaskBusinessEnum businessEnum);

    /**
     * 根据任务的 businessId
     * @param attachmentId
     * @param taskBusinessEnum
     * @param businessId
     * @param editFlag
     */
    void setFileZBusinessId(Long attachmentId, FileZTaskBusinessEnum taskBusinessEnum, Long businessId,String editFlag);
}
