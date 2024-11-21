package com.zhaocai.common.core.utils;
import com.deepoove.poi.XWPFTemplate;
import com.itextpdf.text.*;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

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
        final File tempFile = new File(fileName);
        InputStream fontTempStream = null;
        try {
            fontTempStream = FreeMarkUtils.class.getClassLoader().getResourceAsStream(fileName);
            FileUtils.copyInputStreamToFile(fontTempStream, tempFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fontTempStream != null) {
                    fontTempStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempFile;
    }


    /**
     * 插入图片水印
     * @param srcByte 已生成PDF的字节数组（流转字节）
     * @param destFile 生成有水印的临时文件 temp.pdf
     * @return
     */
    public static FileOutputStream addWaterMark(byte[] srcByte, String destFile) {
        // 待加水印的文件
        PdfReader reader = null;
        // 加完水印的文件
        PdfStamper stamper = null;
        FileOutputStream fileOutputStream = null;
        try {
            reader = new PdfReader(srcByte);
            fileOutputStream = new FileOutputStream(destFile);
            stamper = new PdfStamper(reader, fileOutputStream);
            int total = reader.getNumberOfPages() + 1;
            PdfContentByte content;
            // 设置字体
            //BaseFont font = BaseFont.createFont();
            // 循环对每页插入水印
            for (int i = 1; i < total; i++) {
                final PdfGState gs = new PdfGState();
                // 水印的起始
                content = stamper.getUnderContent(i);
                // 开始
                content.beginText();
                // 设置颜色 默认为蓝色
                //content.setColorFill(BaseColor.BLUE);
                // content.setColorFill(Color.GRAY);
                // 设置字体及字号
                //content.setFontAndSize(font, 38);
                // 设置起始位置
                // content.setTextMatrix(400, 880);
                //content.setTextMatrix(textWidth, textHeight);
                // 开始写入水印
                //content.showTextAligned(Element.ALIGN_LEFT, text, textWidth, textHeight, 45);

                // 设置水印透明度
                // 设置笔触字体不透明度为0.4f
                gs.setStrokeOpacity(0f);
                Image image = null;
                image = Image.getInstance("url");
                // 设置坐标 绝对位置 X Y 这个位置大约在 A4纸 右上角展示LOGO
                image.setAbsolutePosition(472, 785);
                // 设置旋转弧度
                image.setRotation(0);// 旋转 弧度
                // 设置旋转角度
                image.setRotationDegrees(0);// 旋转 角度
                // 设置等比缩放 图片大小
                image.scalePercent(4);// 依照比例缩放
                // image.scaleAbsolute(200,100);//自定义大小
                // 设置透明度
                content.setGState(gs);
                // 添加水印图片
                content.addImage(image);
                // 设置透明度
                content.setGState(gs);
                //结束设置
                content.endText();
                content.stroke();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            try {
                stamper.close();
                fileOutputStream.close();
                reader.close();
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileOutputStream;
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

        String outPath= FreeMarkUtils.createDocx(params, "templates/2.docx","D:\\results\\");

        System.out.println(outPath);


        sealInWord(outPath,
                "D:\\mytestWithImg.docx",
                "D:\\gz.png", "参赛人员须知", 80, 100,
                375, -198, false);


        String pdfPath=FreeMarkUtils.convertDocx2Pdf(outPath,"D:\\results\\");
        System.out.println(pdfPath);


        //String fileName = UUID.randomUUID().toString() + "_001_test.pdf";

        //OutputStream outputStream = new FileOutputStream(fileName);
        //template.write(outputStream);
    }

}