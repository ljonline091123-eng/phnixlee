package com.zhaocai.business.common.utils;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.StreamOpenOfficeDocumentConverter;
import com.deepoove.poi.xwpf.NiceXWPFDocument;
import com.zhaocai.business.common.config.MinioConfig;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.file.FileTypeUtils;
import com.zhaocai.common.core.utils.uuid.Seq;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.zhaocai.business.pub.service.ISysFileService;



public class LibToPdf {


    private static String libreoffceLocation;


    public static String getLibreoffceLocation() {
        return libreoffceLocation;
    }

    public static void setLibreoffceLocation(String libreoffceLocation) {
        LibToPdf.libreoffceLocation = libreoffceLocation;
    }


    private static Integer libreoffceProt;
    public static Integer getLibreoffceProt() {
        return libreoffceProt;
    }

    public static void setLibreoffceProt(Integer libreoffceProt) {
        LibToPdf.libreoffceProt = libreoffceProt;
    }


    /**
     * 文档转换为输入流
     * @param doc
     * @return
     * @throws IOException
     */
    public static  ByteArrayInputStream getNiceXWPFDocByInputStream(NiceXWPFDocument doc) throws IOException {
        // 获取文档对象
        XWPFDocument document = doc.getXWPFDocument();
        //二进制OutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);//文档写入流

        //OutputStream写入InputStream二进制流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        return inputStream;
    }

    public static  void doDocumentConvert(InputStream inputStream, OutputStream outputStream, String form, String to) {
        // 建立连接，根据配置文件获取
        SocketOpenOfficeConnection connection = new SocketOpenOfficeConnection(LibToPdf.getLibreoffceLocation(), LibToPdf.getLibreoffceProt());
        try {
            connection.connect();
            System.out.println("获取连接成功！");
        } catch (ConnectException e) {
            System.out.println("获取连接失败！");
            e.printStackTrace();
        }
        // 转换
        StreamOpenOfficeDocumentConverter converter = new StreamOpenOfficeDocumentConverter(connection);
        // 转换格式
        DocumentFormat docDocumentFormat = (new DefaultDocumentFormatRegistry()).getFormatByFileExtension(form);
        DocumentFormat pdfDocumentFormat = (new DefaultDocumentFormatRegistry()).getFormatByFileExtension(to);

        // 多种转换方式，文件方式，流方式
        converter.convert(inputStream,docDocumentFormat, outputStream,pdfDocumentFormat);
        // 关闭连接
        connection.disconnect();
    }

    public static void main(String[] args) throws Exception{
        InputStream input=new FileInputStream("d:\\mytestWithImg.docx");
        NiceXWPFDocument doc=new NiceXWPFDocument(input);

        String outPutfileName = UUID.randomUUID().toString() + ".pdf";
        String targetPath = "E:\\results\\" +"examFileWithPicture"+ outPutfileName;
//        File file = new File(targetPath);
        Path path = Paths.get(targetPath);
        String contentType = Files.probeContentType(path);
        System.out.println(contentType);
        File resultsDir = new File("E:\\results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        OutputStream outputStream = new FileOutputStream(targetPath);
        ByteArrayInputStream inputStream = LibToPdf.getNiceXWPFDocByInputStream(doc);
        try {
            LibToPdf.setLibreoffceLocation("192.168.240.16");
            LibToPdf.setLibreoffceProt(30002);
            LibToPdf.doDocumentConvert(inputStream,outputStream, "docx","pdf");

        } catch (Exception e) {
            e.printStackTrace();
            try {

                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }finally {


        }


    }

}
