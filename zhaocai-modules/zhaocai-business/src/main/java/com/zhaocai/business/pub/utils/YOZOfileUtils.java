package com.zhaocai.business.pub.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class YOZOfileUtils {
    /**
     * 根据提供的URL下载文件，并保存到给定的路径。
     * @param urlStr 文件的URL
     * @param targetPath 保存的目标路径
     */
    public static Path downloadFile(String urlStr, Path targetPath) {
        try (InputStream in = new URL(urlStr).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("文件已下载至: " + targetPath);

        } catch (IOException e) {
            System.err.println("下载文件时发生错误: " + e.getMessage());
        }
        return targetPath;
    }

    /**
     * 创建一个临时文件路径。
     * @param fileName 文件名
     * @return 临时文件路径
     */
    public static Path createTempFilePath(String fileName) {
        try {
            Path tempDir = Paths.get("D:\\tmp");
            // 检查目录是否存在，如果不存在则创建
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            return tempDir.resolve(fileName);
        } catch (Exception e) {
            throw new RuntimeException("创建临时目录失败", e);
        }
    }

    //删除临时文件
    public static void deleteTempFilePath(String FilePath){
        if (FilePath != null) {
            try {
                Files.deleteIfExists(Paths.get(FilePath));
                System.out.println("Temporary file deleted: " + FilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //去掉文件后缀
    public static String removeSuffix(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }



}
