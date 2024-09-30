package com.zhaocai.business.pub.service.impl;

import com.zhaocai.business.common.config.MinioConfig;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.business.common.utils.FileUploadUtils;
import com.zhaocai.business.pub.service.ISysFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minio 文件存储
 *
 * @author chenming
 */
@Slf4j
@Service
public class MinioSysFileServiceImpl implements ISysFileService {
    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient minioClient;

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file){
        String fileName = FileUploadUtils.extractFilename(file);
        try(InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(args);

            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
        } catch (Exception e) {
            log.error("文件上传失败,cause by :{}",e.getMessage(),e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR,"上传文件失败",e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream,String fileName) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);

            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
        } catch (Exception e) {
            log.error("文件上传失败,cause by :{}",e.getMessage(),e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR,"上传文件失败",e);
        }
    }

    @Override
    public InputStream getFileByFileUrl(String fileUrl) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(getMinioFileName(fileUrl,minioConfig.getBucketName()))
                    .build();

            return minioClient.getObject(args);
        } catch (Exception e) {
            log.error("下载文件失败,cause by :{}",e.getMessage(),e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR,"下载文件失败",e);
        }
    }

    /**
     *
     * @param fileUrl
     * @param bucketName
     * @return
     */
    private String getMinioFileName(String fileUrl,String bucketName) {
        String patternStr = bucketName + "/(.*)";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(fileUrl);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new BusinessException(ResultCode.FAILURE,"获取文件路径失败");
        }
     }
}
