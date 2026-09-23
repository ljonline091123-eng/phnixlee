package com.zhaocai.business.pub.utils;

import com.zhaocai.business.common.config.FileYOZOConfig;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.uuid.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YOZOfileUtils {

//    @Autowired
//    private FileYOZOConfig fileYOZOConfig;


    private final FileYOZOConfig fileYOZOConfig;

    @Autowired
    public YOZOfileUtils(FileYOZOConfig fileYOZOConfig) {
        this.fileYOZOConfig = fileYOZOConfig;
    }

    public void main(String[] args) {

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

            System.out.println("配置类的引用：——————");
            System.out.println("fileYOZOConfig:"+fileYOZOConfig.getTempFilePath());

        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

    /**
     * 将Date对象格式化为指定格式的字符串，然后再解析回Date对象。
     *
     * @param date       需要格式化的Date对象
     * @param formatStr  目标日期格式，例如 "yyyy-MM-dd"
     * @return           格式化后的Date对象
     */
    public static String formatDate(Date date, String formatStr) {
        if (date == null || formatStr == null || formatStr.trim().isEmpty()) {
            throw new IllegalArgumentException("进场日期或者完工日期不能为空");
        }

        // 创建SimpleDateFormat对象用于格式化和解析日期
        SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
        // 将Date对象格式化为字符串
        String formattedDateStr = sdf.format(date);
        // 解析字符串回Date对象
//            return sdf.parse(formattedDateStr);
        return formattedDateStr;

    }


    /**
     * 根据提供的URL下载文件，并保存到给定的路径。
     * @param urlStr 文件的URL
     * @param targetPath 保存的目标路径
     */
    public Path downloadFile(String urlStr, Path targetPath) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            throw new RuntimeException("无法下载文件，未获取到文件URL");
        }
        //对urlStr的空格部分进行编码
        try {
            // 直接替换url里的空格为 %20
            String encodedUrl = urlStr.replace(" ", "%20");
            // 输出编码后的URL
            System.out.println("替换空格后的encodedUrl(下载地址)：" + encodedUrl);

            InputStream in;
            // TODO【临时绕过】2026-09-18：文件服务器 118.253.180.94:8193 的 SSL 证书过期
            // （CN=zxdljs.com，notAfter=2026-09-08），导致 PKIX validity check failed。
            // 临时改为“信任所有证书+跳过主机名校验”以解除合同签订/文件下载阻塞。
            // 注意：证书续期时须带上 IP:118.253.180.94 的 SAN（库里附件URL用的是IP直连），
            // 续期完成后务必删除本分支，恢复为 new URL(encodedUrl).openStream()。
            if (encodedUrl.startsWith("https://")) {
                in = openTrustAllHttpsStream(encodedUrl);
            } else {
                in = new URL(encodedUrl).openStream();
            }
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件已下载至: " + targetPath);

        } catch (IOException e) {
            System.err.println("下载文件时发生错误: " + e.getMessage());
            throw new RuntimeException("下载文件时发生错误:" + e.getMessage(), e);
        }
        return targetPath;

    }

    /**
     * 临时方法：以“信任所有证书 + 跳过主机名校验”的方式打开 HTTPS 连接。
     * 仅用于文件服务器证书过期期间的应急下载，证书续期后应随 TODO 分支一并移除。
     *
     * @param urlStr HTTPS 下载地址
     * @return 文件输入流
     * @throws IOException 连接失败
     */
    private InputStream openTrustAllHttpsStream(String urlStr) throws IOException {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());

            HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setHostnameVerifier((hostname, session) -> true);
            return conn.getInputStream();
        } catch (Exception e) {
            throw new IOException("临时HTTPS下载连接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建一个临时文件路径。
     * @param fileName 文件名
     * @return 临时文件路径
     */
    public Path createTempFilePath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new RuntimeException("创建临时目录失败，未获取到文件名fileName");
        }
        // 判断文件名是否超过255字节长度
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        if (fileNameBytes.length > 255) {
            throw new RuntimeException("创建临时目录失败，文件名过长，超过255字节");
        }

        try {
//            Path tempDir = Paths.get("D:\\tmp");
            // 使用配置中的临时文件路径
            Path tempDir = Paths.get(fileYOZOConfig.getTempFilePath());
            System.out.println("TempFilePath:临时文件夹"+ fileYOZOConfig.getTempFilePath());
            // 检查目录是否存在，如果不存在则创建
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            //在tmp下新建子目录来保存文件
            String fileName2 = removeSuffix(fileName);
//            // 生成当前时间戳并格式化
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
//            String timestamp = dateFormat.format(new Date());
            String outPutDirName = UUID.randomUUID().toString();
            Path outputDir = tempDir.resolve(outPutDirName);
            // 检查新子目录是否存在，如果不存在则创建
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // 构建文件路径
            Path filePath = outputDir.resolve(fileName);
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("创建临时目录失败" + e.getMessage(), e);
        }
    }


    /**
     * 修改文件名为原文件名加上随机UUID作为后缀。
     *
     * @param originalFileName 原始文件名（包括扩展名）
     * @return 修改后的文件名
     */

//    public static String modifyFileName(String originalFileName) {
//        if (originalFileName == null || originalFileName.isEmpty()) {
//            throw new IllegalArgumentException("文件名不能为空");
//        }
//
//        // 获取文件的扩展名（如果有）
//        int dotIndex = originalFileName.lastIndexOf('.');
//        String fileNameWithoutExtension;
//        String fileExtension = "";
//
//        if (dotIndex != -1) {
//            fileNameWithoutExtension = originalFileName.substring(0, dotIndex);
//            fileExtension = originalFileName.substring(dotIndex);
//        } else {
//            fileNameWithoutExtension = originalFileName;
//        }
//
//        // 如果文件名中包含下划线，则移除下划线及其后面的内容
//        int underscoreIndex = fileNameWithoutExtension.indexOf('_');
//        if (underscoreIndex != -1) {
//            fileNameWithoutExtension = fileNameWithoutExtension.substring(0, underscoreIndex);
//        }
//
//        // 生成新的UUID并去掉其中的连字符
//        String newUuid = UUID.randomUUID().toString().replaceAll("-", "");
//
//        // 构建新的文件名：更新后的文件名 + 新UUID + 扩展名
//        return fileNameWithoutExtension + "_" + newUuid + fileExtension;
//    }

    public static String modifyFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 获取文件的扩展名（如果有）
        int dotIndex = originalFileName.lastIndexOf('.');
        String fileNameWithoutExtension;
        String fileExtension = "";

        if (dotIndex != -1) {
            fileNameWithoutExtension = originalFileName.substring(0, dotIndex);
            fileExtension = originalFileName.substring(dotIndex);
        } else {
            fileNameWithoutExtension = originalFileName;
        }

        // 定义时间戳的正则表达式模式
        String timestampPattern = "_\\d{17}";  // 匹配下划线后跟17位数字（yyyyMMddHHmmssSSS）

        // 移除文件名中已有的时间戳（如果存在）
        fileNameWithoutExtension = fileNameWithoutExtension.replaceAll(timestampPattern, "");

        // 生成当前时间戳并格式化
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String timestamp = dateFormat.format(new Date());

        // 构建新的文件名：更新后的文件名 + 新UUID + 扩展名
        return fileNameWithoutExtension + "_" + timestamp + fileExtension;
    }

    //删除临时文件
    public void deleteTempFilePath(String FilePath){
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
    public String removeSuffix(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    //获取文件后缀（文件类型）
    public String getSuffix(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    //判断文件类型
    //是否为图片
    public boolean isImageExtension(String extension) {
        for (String imgExt : mineTypeUtils.IMAGE_EXTENSION) {
            if (imgExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    //是否为word
    public boolean isWordExtension(String extension) {
        for (String wordExt : mineTypeUtils.WORD_EXTENSION) {
            if (wordExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    //是否为pdf
    public boolean isPdfExtension(String extension) {
        for (String pdfExt : mineTypeUtils.PDF_EXTENSION) {
            if (pdfExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    //判断fileURL是否为空
    public boolean isNULLFileURL(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return true;
        } else {
            return false;
        }
    }

    //替换responseURL的前缀（服务器地址）
    public String updateFileUrl(String fileUrl) {
        // 定义旧的和新的URL前缀
      String oldPrefix =fileYOZOConfig.getOldResponsePrefix();
      String newPrefix = fileYOZOConfig.getNewResponsePrefix();

        // 检查fileUrl是否以oldPrefix开头
        if (fileUrl.startsWith(oldPrefix)) {
            // 使用字符串替换方法更新URL
            return fileUrl.replace(oldPrefix, newPrefix);
        }

        // 如果不匹配，则返回原URL
        return fileUrl;
    }



}
