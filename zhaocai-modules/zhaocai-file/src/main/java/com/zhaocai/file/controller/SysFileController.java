package com.zhaocai.file.controller;

import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.utils.file.FileUtils;
import com.zhaocai.file.service.ISysFileService;
import com.zhaocai.system.api.domain.SysFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 文件请求处理
 *
 * @author ruoyi
 */
@Slf4j
@RestController
public class SysFileController {
    @Autowired
    private ISysFileService sysFileService;

    /**
     * 文件上传请求
     */
    @PostMapping("/upload")
    public R<SysFile> upload(MultipartFile file) {
        try {
            // 获取原始文件名并检查其字节长度
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return R.fail("无效的文件名");
            }

            byte[] fileNameBytes = originalFilename.getBytes(StandardCharsets.UTF_8);
            if (fileNameBytes.length > 255) {
                return R.fail("文件名过长，超过255字节，上传文件失败，请重新上传文件");
            }

            // 上传并返回访问地址
            String url = sysFileService.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);
        }
        catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }
}
