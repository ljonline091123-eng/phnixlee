package com.zhaocai.business.pub.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件接口
 *
 * @author chenming
 */
public interface ISysFileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传文件
     * @param inputStream
     * @param fileName
     * @return
     */
    String uploadFile(InputStream inputStream,String fileName);

    /**
     * 下载文件
     * @param fileUrl
     * @return
     */
    InputStream getFileByFileUrl(String fileUrl);
}
