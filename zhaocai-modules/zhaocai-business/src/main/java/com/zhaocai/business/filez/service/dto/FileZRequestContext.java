package com.zhaocai.business.filez.service.dto;


import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.filez.dto.FileZRequestDTO;
import com.zhaocai.business.filez.dto.FileZResponseDTO;
import com.zhaocai.common.core.utils.NumberUtil;
import lombok.Data;

/**
 * 联想文档接口请求上下文
 *
 * @author chenming
 * @date 2024-07-05
 */
@Data
public class FileZRequestContext {

    /**
     * 业务编码
     */
    private FileZTaskBusinessEnum businessCodeEnum;

    /**
     * 请求操作
     */
    private FileZRequestTypeEnum requestTypeEnum;

    /**
     * 业务 id
     */
    private Long businessId;

    /**
     * 文档任务 id
     */
    private Long fileZTaskId;

    /**
     * 联想文档请求操作
     */
    private FileZRequestDTO fileZRequest;

    /**
     * 联想文档响应实体
     */
    private FileZResponseDTO fileZResponse;

    public FileZRequestContext(FileZRequestTypeEnum requestTypeEnum,FileZTaskBusinessEnum businessCodeEnum,Long businessId) {
        if (requestTypeEnum == null) {
            throw new ParamValidateException("请求操作类型不能为空");
        }
        if (businessCodeEnum == null) {
            throw new ParamValidateException("业务编码不能为空");
        }
        if (NumberUtil.isNullOrZero(businessId)) {
            throw new ParamValidateException("业务 id不能为空");
        }

        this.requestTypeEnum = requestTypeEnum;
        this.businessCodeEnum = businessCodeEnum;
        this.businessId = businessId;
    }
}
