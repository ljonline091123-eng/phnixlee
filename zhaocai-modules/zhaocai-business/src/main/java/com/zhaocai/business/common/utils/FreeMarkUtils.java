package com.zhaocai.business.common.utils;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.xwpf.NiceXWPFDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * pdf 导出工具类
 */
@Component
@Slf4j
public final class FreeMarkUtils {

    /**
     * 根据docx模板填充数据  并生成pdf文件
     *
     * @param dataMap      数据源
     * @param docxFile     docx模板的文件名
     * @return 生成的文件路径
     */
    public static String createDocx(Map<String, Object> dataMap, String docxFile,String outPath) {
        //输出word文件路径和名称 (临时文件名，本次为测试，最好使用雪花算法生成，或者用uuid)
        String fileName = outPath+UUID.randomUUID().toString() + ".docx";

        // word 数据填充
        // 生成docx临时文件
        final File tempPath = new File(fileName);
        final File docxTempFile = getTempFile(docxFile);
        if (docxTempFile == null) {
            System.err.println("Failed to load template file: " + docxFile);
            return null;
        }
        XWPFTemplate template = XWPFTemplate.compile(docxTempFile).render(dataMap);
        try {
            template.write(new FileOutputStream(tempPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //返回生成的文件路径
        return fileName;
    }

    /**
     * word（doc）转pdf
     *
     * @param
     * @return 生成的带水印的pdf路径
     */
    public static String convertDocx2Pdf(String filePath,String outPath) {

       return "";
    }





    /**
     * 文件转字节输出流
     *
     * @param outFile 文件
     * @return
     */
    public static ByteArrayOutputStream getFileOutputStream(File outFile) {
        // 获取生成临时文件的输出流
        InputStream input = null;
        ByteArrayOutputStream bytestream = null;
        try {
            input = new FileInputStream(outFile);
            bytestream = new ByteArrayOutputStream();
            int ch;
            while ((ch = input.read()) != -1) {
                bytestream.write(ch);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bytestream.close();
                input.close();
                log.info("删除临时文件");
                if (outFile.exists()) {
                    outFile.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytestream;
    }

    /**
     * 获取资源文件的临时文件
     * 资源文件打jar包后，不能直接获取，需要通过流获取生成临时文件
     *
     * @param fileName 文件路径 templates/xxx.docx
     * @return
     */
    public static File getTempFile(String fileName) {

        // 创建一个临时文件来存储模板内容
        File tempFile = null;
        InputStream templateStream = null;
        try {
            // 检查文件是否存在
            File originalFile = new File(fileName);
            if (!originalFile.exists()) {
                System.err.println("Original file not found: " + fileName);
                return null;
            }

            // 创建临时文件
            File parentDir = new File("D:\\results\\");
            parentDir.mkdirs(); // 确保目录存在

            tempFile = File.createTempFile("template", ".docx", parentDir);
            System.out.println("Temporary file created at: " + tempFile.getAbsolutePath());
            templateStream = new FileInputStream(originalFile);
            FileUtils.copyInputStreamToFile(templateStream, tempFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (templateStream != null) {
                    templateStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempFile;
    }







    /**
     * <b> Word中添加图章
     * </b><br><br><i>Description</i> :
     * String srcPath, 源Word路径
     * String storePath, 添加图章后的路径
     * String sealPath, 图章路径（即图片）
     * tString abText, 在Word中盖图章的标识字符串，如：(签字/盖章)
     * int width, 图章宽度
     * int height, 图章高度
     * int leftOffset, 图章在编辑段落向左偏移量
     * int topOffset, 图章在编辑段落向上偏移量
     * boolean behind，图章是否在文字下面
     *
     * @return void
     * <br><br>Date: 2019/12/26 15:12     <br>Author : dxl
     */
    public static void sealInWord(String srcPath, String storePath, String sealPath, String tabText,
                                  int width, int height, int leftOffset, int topOffset, boolean behind) throws Exception {
        File fileTem = new File(srcPath);
        InputStream is = new FileInputStream(fileTem);
        XWPFDocument document = new XWPFDocument(is);
        java.util.List< org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = document.getParagraphs();
        XWPFRun targetRun = null;
        for (XWPFParagraph paragraph : paragraphs) {
            System.out.println(paragraph.getText());
            if (!"".equals(paragraph.getText()) && paragraph.getText().contains(tabText)) {
                java.util.List<XWPFRun> runs = paragraph.getRuns();
                targetRun = runs.get(runs.size() - 1);
            }
        }
        if (targetRun != null) {
            InputStream in = new FileInputStream(sealPath);//设置图片路径
            if (width <= 0) {
                width = 100;
            }
            if (height <= 0) {
                height = 100;
            }
            //创建Random类对象
            Random random = new Random();
            //产生随机数
            int number = random.nextInt(999) + 1;
            targetRun.addPicture(in, XWPFDocument.PICTURE_TYPE_PNG, "Seal" + number, Units.toEMU(width), Units.toEMU(height));
            in.close();
            // 2. 获取到图片数据
            CTDrawing drawing = targetRun.getCTR().getDrawingArray(0);
            CTGraphicalObject graphicalobject = drawing.getInlineArray(0).getGraphic();

            //拿到新插入的图片替换添加CTAnchor 设置浮动属性 删除inline属性
            CTAnchor anchor = getAnchorWithGraphic(graphicalobject, "Seal" + number,
                    Units.toEMU(width), Units.toEMU(height),//图片大小
                    Units.toEMU(leftOffset), Units.toEMU(topOffset), behind);//相对当前段落位置 需要计算段落已有内容的左偏移
            drawing.setAnchorArray(new CTAnchor[]{anchor});//添加浮动属性
            drawing.removeInline(0);//删除行内属性
        }
        document.write(new FileOutputStream(storePath));
        document.close();
    }

    /**
     * @param ctGraphicalObject 图片数据
     * @param deskFileName      图片描述
     * @param width             宽
     * @param height            高
     * @param leftOffset        水平偏移 left
     * @param topOffset         垂直偏移 top
     * @param behind            文字上方，文字下方
     * @return
     * @throws Exception
     */
    public static CTAnchor getAnchorWithGraphic(CTGraphicalObject ctGraphicalObject,
                                                String deskFileName, int width, int height,
                                                int leftOffset, int topOffset, boolean behind) {
        System.out.println(">>width>>" + width + "; >>height>>>>" + height);
        String anchorXML =
                "<wp:anchor xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
                        + "simplePos=\"0\" relativeHeight=\"0\" behindDoc=\"" + ((behind) ? 1 : 0) + "\" locked=\"0\" layoutInCell=\"1\" allowOverlap=\"1\">"
                        + "<wp:simplePos x=\"0\" y=\"0\"/>"
                        + "<wp:positionH relativeFrom=\"column\">"
                        + "<wp:posOffset>" + leftOffset + "</wp:posOffset>"
                        + "</wp:positionH>"
                        + "<wp:positionV relativeFrom=\"paragraph\">"
                        + "<wp:posOffset>" + topOffset + "</wp:posOffset>" +
                        "</wp:positionV>"
                        + "<wp:extent cx=\"" + width + "\" cy=\"" + height + "\"/>"
                        + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                        + "<wp:wrapNone/>"
                        + "<wp:docPr id=\"1\" name=\"Drawing 0\" descr=\"" + deskFileName + "\"/><wp:cNvGraphicFramePr/>"
                        + "</wp:anchor>";

        CTDrawing drawing = null;
        try {
            drawing = CTDrawing.Factory.parse(anchorXML);
        } catch (XmlException e) {
            e.printStackTrace();
        }
        CTAnchor anchor = drawing.getAnchorArray(0);
        anchor.setGraphic(ctGraphicalObject);
        return anchor;
    }

    public static void main(String[] args) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("name","谢文静");
        params.put("card","411111212121596621");
        params.put("dep","交科天颐");
        params.put("no","236521");
        params.put("no1","12");
        params.put("no2","01");

        //生成没有图片的文档
        String outPath= FreeMarkUtils.createDocx(params, "templates/2.docx","D:\\results\\");

        System.out.println("outPath  " +outPath);

        String url = "http://192.168.30.42:9000/wh-hnjt/2024/11/25/微信图片_20241112093225_20241125163104A050.jpg";
        String PicturePath = FreeMarkUtils.downloadFileFromUrl(url);

        //生成有头像的文档,源路径，添加图章后的路径
        String outPutfileName = UUID.randomUUID().toString() + ".docx";
        String sropath = "D:\\results\\" + outPutfileName;
        sealInWord(outPath,
                sropath,
                PicturePath, "参赛人员须知", 80, 100,
                375, -197, false);

        //将word文档转换为pdf格式
        InputStream input=new FileInputStream(sropath);
        NiceXWPFDocument doc=new NiceXWPFDocument(input);
        String PDFfileName = UUID.randomUUID().toString() + ".pdf";
        String PDFtargetPath = "D:\\results\\" +"examPDF"+ PDFfileName;
        OutputStream outputStream = new FileOutputStream(PDFtargetPath);
        ByteArrayInputStream inputStream = LibToPdf.getNiceXWPFDocByInputStream(doc);
        try {
            LibToPdf.setLibreoffceLocation("192.168.30.240");
            LibToPdf.setLibreoffceProt(8989);
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



        System.out.println("loadPicturePath: " + PicturePath);


    }

    /**
     * 下载文件到指定目录
     *
     * @param url URL
     * @return 本地文件路径
     * @throws IOException
     */
    public static String downloadFileFromUrl(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL不能为空");
        }

        // 获取文件扩展名
        String extension = FilenameUtils.getExtension(url);
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("URL中缺少文件扩展名");
        }

        // 指定目标目录
        File targetDir = new File("D:\\results\\");
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (!created) {
                throw new IOException("无法创建目标目录: " + targetDir.getAbsolutePath());
            }
        }

        // 创建临时文件
        File tempFile = File.createTempFile("temp", "." + extension, targetDir);
        FileUtils.copyURLToFile(new URL(url), tempFile);

        return tempFile.getAbsolutePath();
    }



}