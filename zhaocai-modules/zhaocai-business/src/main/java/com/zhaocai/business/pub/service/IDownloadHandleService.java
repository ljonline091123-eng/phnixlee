package com.zhaocai.business.pub.service;


import com.zhaocai.business.common.enums.AttachmentTypeEnum;

/**
 * 文件下载处理服务
 *
 * @author chenming
 * @date 2024-07-24
 */
public interface IDownloadHandleService {

    /**
     * 文件下载
     * @param bytes
     * @param businessType
     * @param businessId
     * @param fileName
     */
    Long downloadHandle(byte[] bytes, AttachmentTypeEnum businessType, long businessId,String fileName);
}
