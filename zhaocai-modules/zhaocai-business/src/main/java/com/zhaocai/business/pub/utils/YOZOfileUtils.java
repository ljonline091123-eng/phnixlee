package com.zhaocai.business.pub.utils;

import com.zhaocai.common.core.utils.uuid.UUID;

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
            //在tmp下新建子目录来保存文件
            String fileName2 = YOZOfileUtils.removeSuffix(fileName);
            String outPutDirName = fileName2 + "_"+UUID.randomUUID().toString();
//            String outPutDirName = fileName2;
            Path outputDir = tempDir.resolve(outPutDirName);
            // 检查新子目录是否存在，如果不存在则创建
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // 构建文件路径
            Path filePath = outputDir.resolve(fileName);
            return filePath;
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

    //获取文件后缀（文件类型）
    public static String getSuffix(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    //判断文件类型
    //是否为图片
    public static boolean isImageExtension(String extension) {
        for (String imgExt : mineTypeUtils.IMAGE_EXTENSION) {
            if (imgExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    //是否为word
    public static boolean isWordExtension(String extension) {
        for (String wordExt : mineTypeUtils.WORD_EXTENSION) {
            if (wordExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    //是否为pdf
    public static boolean isPdfExtension(String extension) {
        for (String pdfExt : mineTypeUtils.PDF_EXTENSION) {
            if (pdfExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }


    public static void main(String[] args) {
        try {
            String suffix = getSuffix("example.JPG").toLowerCase();
            System.out.println("suffix:: " + suffix);
            if(isImageExtension(suffix)){
                System.out.println("是图片");
            } else if (isPdfExtension(suffix)) {
                System.out.println("是PDF");
            } else if (isWordExtension(suffix)) {
                System.out.println("是word");
            }
            String fileName = "建设工程施工专业分包合同.docx";
            String fileUrl = "http://192.168.240.21:9000/wh-hnjt/2024/10/25/1-建设工程施工专业分包合同_20240827091758A105_20241025151420A036.docx";
            Path targetPath = createTempFilePath(fileName);
            System.out.println("filepath:" + targetPath);
            Path path1 = downloadFile(fileUrl,targetPath);

        } catch (RuntimeException e) {
            e.printStackTrace();
        }


    }



}
