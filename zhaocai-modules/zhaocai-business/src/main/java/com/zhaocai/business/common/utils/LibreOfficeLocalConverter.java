package com.zhaocai.business.common.utils;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class LibreOfficeLocalConverter {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String LIBREOFFICE_PATH = detectLibreOfficePath();

    /**
     * 使用本地LibreOffice将DOCX转换为PDF
     */
    public static byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
        Path tempInputFile = null;
        Path tempOutputFile = null;

        try {
            // 创建临时文件
            String tempFileName = "doc_" + UUID.randomUUID().toString() + ".docx";
            tempInputFile = Paths.get(TEMP_DIR, tempFileName);

            // 写入Word内容到临时文件
            Files.write(tempInputFile, docxBytes);

            // 调用LibreOffice进行转换
            ProcessBuilder pb = new ProcessBuilder(
                    LIBREOFFICE_PATH,
                    "--headless",
                    "--convert-to", "pdf:writer_pdf_Export",
                    "--outdir", TEMP_DIR,
                    tempInputFile.toString()
            );

            Process process = pb.start();

            // 读取错误流信息
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            StringBuilder errorMsg = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorMsg.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("PDF转换失败: " + errorMsg.toString());
            }

            // 查找生成的PDF文件
            String pdfFileName = tempFileName.replace(".docx", ".pdf");
            tempOutputFile = Paths.get(TEMP_DIR, pdfFileName);

            if (!Files.exists(tempOutputFile)) {
                throw new RuntimeException("PDF文件未生成");
            }

            // 读取PDF内容
            return Files.readAllBytes(tempOutputFile);

        } finally {
            // 清理临时文件
            if (tempInputFile != null) Files.deleteIfExists(tempInputFile);
            if (tempOutputFile != null) Files.deleteIfExists(tempOutputFile);
        }
    }

    /**
     * 检测LibreOffice安装路径
     */
    private static String detectLibreOfficePath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "C:\\Program Files\\LibreOffice\\program\\soffice.exe";
        } else if (os.contains("mac")) {
            return "/Applications/LibreOffice.app/Contents/MacOS/soffice";
        } else {
            return "soffice";
        }
    }

    /**
     * 检查LibreOffice是否可用
     */
    public static boolean isLibreOfficeAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(LIBREOFFICE_PATH, "--version");
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
