package com.zhaocai.business.poi.service.Impl;

import com.alibaba.nacos.common.packagescan.resource.ClassPathResource;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.agreement.vo.res.*;
import com.zhaocai.business.common.utils.LibToPdf;
import com.zhaocai.business.common.utils.LibreOfficeLocalConverter;
import com.zhaocai.business.common.utils.ObjectFormatterUtils;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.service.BpmService;
import com.zhaocai.business.poi.enums.WordAgreementTypeEnum;
import com.zhaocai.business.poi.service.WordService;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanDetailVO;
import com.zhaocai.business.pub.domain.Attachment;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

@Slf4j
@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private BpmService bpmService;

    @Autowired
    private IProcurementPlanService procurementPlanService;

    final private static String urlAgreement = "https://ck.hncig.cn:32000/ckControl/zbcg/procurement/procurement$sign-contract?wjSs=%2Fzhaocai%2Fprocurement%2Fcontract-detail%2F";

    final private static String urlAgreementPlan = "http://127.0.0.1/zhaocai/procurement/plan-detail/";


    /**
     *  下载链接需要设置网关白名单
     *  http://127.0.0.1:8052/business/agreementWord/generate?id=1888879000955854849
     *
     */
    @Override
    public void generateWord(Long id, String type, HttpServletResponse response) throws Exception {
        /* word用的数据格式替换对象 */
        Map<String, Object> dataModel = new HashMap<>();
        AgreementDetailVO agreementDetailVO = agreementService.detail(id);
        /* 格式化 响应数据 主要是处理继承了AdviceObject对象的注解@** */
        ObjectFormatterUtils.format(agreementDetailVO);

        /* 获取审批流程 */
        BpmListProcessLogRequestDTO requestDTO  = new BpmListProcessLogRequestDTO();
        requestDTO.setBusinessId(agreementDetailVO.getAgreement().getId().toString());
        requestDTO.setProcessId(agreementDetailVO.getAgreement().getWfProcessId());
        List<BpmListProcessLogResponseDTO> listBpm = bpmService.listProcessLog(requestDTO);
        /* 反转列表 */
        Collections.reverse(listBpm);
//        extractedBpmList(listBpm, dataModel);// 这个还是正常表格那样循环显示
//        dataModel.put("bpmLists", bpmLists);
        List<BpmListProcessLogResponseDTO> newBpmList = new ArrayList<>();
        int mergeCounter = 1;
        for (BpmListProcessLogResponseDTO log : listBpm) {
            // 如果操作名称为 "重新提交"，则插入一条合并行数据
            if ("重新提交".equals(log.getOperateName())) {
                mergeCounter++;
                BpmListProcessLogResponseDTO mergeLog1 = new BpmListProcessLogResponseDTO();
                // 根据你的实际字段，设置一个标识或描述（如合并行）
                mergeLog1.setTaskName("");
                mergeLog1.setOperateName("");
                mergeLog1.setOperateRemark("");
                mergeLog1.setOperateComment("");
                mergeLog1.setStartTime("");
                mergeLog1.setEndTime("");
                mergeLog1.setHandlerName("");
                newBpmList.add(mergeLog1);

                BpmListProcessLogResponseDTO mergeLog2 = new BpmListProcessLogResponseDTO();
                mergeLog2.setTaskName("⚫⚫第" + mergeCounter + "次提交⚫⚫");
                mergeLog2.setOperateName("");
                mergeLog2.setOperateRemark("");
                mergeLog2.setOperateComment("");
                mergeLog2.setStartTime("");
                mergeLog2.setEndTime("");
                mergeLog2.setHandlerName("");
                newBpmList.add(mergeLog2);

                BpmListProcessLogResponseDTO mergeLog3 = new BpmListProcessLogResponseDTO();
                mergeLog3.setTaskName("");
                mergeLog3.setOperateName("");
                mergeLog3.setOperateRemark("");
                mergeLog3.setOperateComment("");
                mergeLog3.setStartTime("");
                mergeLog3.setEndTime("");
                mergeLog3.setHandlerName("");
                newBpmList.add(mergeLog3);
            }
            // 添加原始日志信息
            newBpmList.add(log);
        }
        // 将新列表赋值给 dataModel 对应的 key
        dataModel.put("bpmLists", newBpmList);

        /* 提取操作名称为“提交”的那一条 */
        for (BpmListProcessLogResponseDTO item : listBpm) {
            if ("提交".equals(item.getOperateName())) {
                /* 编制人就是提交人 */
                dataModel.put("bianZhiRen", item.getHandlerName());
                /* 发起日期就是开始时间 */
                dataModel.put("faQiRiQi", item.getStartTime());
                break; // 如果只要第一条，找到就退出
            }
        }


        /* 合同基本信息 */
        dataModel.put("agreement", agreementDetailVO.getAgreement());

        /* 合同款项信息 */
        dataModel.put("agreementPaymentItem", agreementDetailVO.getAgreementPaymentItem());

        /* 格式化 甲乙 签约方 */
        List<AgreementPartyInfoVO> partyList = agreementDetailVO.getAgreementPartyInfoLists();
        for (AgreementPartyInfoVO party : partyList) {
            if ("1".equals(party.getRoleType())) {
                /* 甲方 */
                dataModel.put("partyA", party);
            } else if ("2".equals(party.getRoleType())) {
                /* 乙方 */
                dataModel.put("partyB", party);
            }
        }

        /* 结算与付款节点信息 */
        List<Map<String, Object>> agreementPaymentLists = new ArrayList<>();
        extractedPayment(agreementDetailVO, agreementPaymentLists, dataModel);


        /* 合同清单/物质清单 */
        List<Map<String, Object>> materialsList = new ArrayList<>();
        extractedMaterials(agreementDetailVO, materialsList, dataModel);


        /* 合同押金保证金 */
        List<Map<String, Object>> agreementDeposits = new ArrayList<>();
        extractedDeposits(agreementDetailVO, agreementDeposits, dataModel);


        /* 合同-计日工对象 */
        List<Map<String, Object>> agreementDailyWageList = new ArrayList<>();
        extractedWage(agreementDetailVO, agreementDailyWageList, dataModel);


        /* 合同-机械台班对象 */
        List<Map<String, Object>> agreementMachineShifts = new ArrayList<>();
        extractedShifts(agreementDetailVO, agreementMachineShifts, dataModel);


        /* 合同-甲供设备清单对象 */
        List<Map<String, Object>> agreementEquipmentSupplies = new ArrayList<>();
        extractedEquipment(agreementDetailVO, agreementEquipmentSupplies, dataModel);


        /* 合同-甲供材料清单对象 */
        List<Map<String, Object>> agreementMaterialSupplies = new ArrayList<>();
        extractedSupplies(agreementDetailVO, agreementMaterialSupplies, dataModel);


        /* 合同附件对象 */
        List<Map<String, Object>> agreementAttachmentList = new ArrayList<>();
        extractedAttachment(agreementDetailVO, agreementAttachmentList, dataModel);


        /* 加载模板 */
        ClassPathResource resource = new ClassPathResource(WordAgreementTypeEnum.DEFAULT.getPath());
        try (InputStream inputStream = resource.getInputStream()) {

            /* 需要循环的表数据 */
            LoopRowTableRenderPolicy loopRowTableRenderPolicy = new LoopRowTableRenderPolicy();
            Configure config = Configure.builder()
                    .bind("agreementPaymentLists",loopRowTableRenderPolicy)
                    .bind("materialsList", loopRowTableRenderPolicy)
                    .bind("agreementDeposits", loopRowTableRenderPolicy)
                    .bind("agreementDailyWageList", loopRowTableRenderPolicy)
                    .bind("agreementMachineShifts", loopRowTableRenderPolicy)
                    .bind("agreementEquipmentSupplies", loopRowTableRenderPolicy)
                    .bind("agreementMaterialSupplies", loopRowTableRenderPolicy)
                    .bind("agreementAttachmentList", loopRowTableRenderPolicy)
                    .bind("bpmLists", loopRowTableRenderPolicy)
                    .build();

            XWPFTemplate template = XWPFTemplate.compile(inputStream, config).render(dataModel);
            /* 增加页脚 */
            extractedAddPageHeaderFooter(template, agreementDetailVO);

            /* 判断是导出Pdf还是Docx */
            if(type!=null && (type.equals("word") || type.equals("docx") || type.equals("doc"))){
                /* 设置响应头（支持中文文件名） */
                String fileName = URLEncoder.encode(agreementDetailVO.getAgreement().getAgreementCode()+"招采合同.docx", "UTF-8").replaceAll("\\+", "%20");
                response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
                /* 输出 Word 文件到浏览器 */
                template.writeAndClose(response.getOutputStream());
            }else{
                /* 适合小型文件存储在内存中，文件大于1MB或者以上内存会有压力。 */
                ByteArrayOutputStream wordOut = new ByteArrayOutputStream();
                ByteArrayInputStream wordInput = null;
                ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
                try {
                    template.writeAndClose(wordOut);
                    wordInput = new ByteArrayInputStream(wordOut.toByteArray());

                    /* 这里 form 是 "docx"，to 是 "pdf" */
//                    LibToPdf.setLibreoffceLocation("192.168.240.5");/* 测试环境转换服务，k8s已经指定node节点 */
                    LibToPdf.setLibreoffceLocation("192.168.241.90");/* 正式环境转换服务，k8s已经指定node节点 */
                    LibToPdf.setLibreoffceProt(30002);
                    LibToPdf.doDocumentConvert(wordInput, pdfOut, "docx", "pdf");

                    /* 设置 PDF 响应头（下载 PDF） */
                    String fileName = URLEncoder.encode(agreementDetailVO.getAgreement().getAgreementCode() + "招采合同.pdf", "UTF-8").replaceAll("\\+", "%20");
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);

                    /* 输出 PDF 到浏览器 */
                    pdfOut.writeTo(response.getOutputStream());
                } finally {
                    if (wordOut != null) wordOut.close();
                    if (wordInput != null) wordInput.close();
                    if (pdfOut != null) pdfOut.close();
                }
            }

        }

    }

    @Override
    public void generateWordPlan(Long id, String type, HttpServletResponse response) throws Exception {
        /* word用的数据格式替换对象 */
        Map<String, Object> dataModel = new HashMap<>();

        ProcurementPlanDetailVO procurementPlanDetail = procurementPlanService.getProcurementPlanDetail(id);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if(procurementPlanDetail.getProcurementPlan() != null ){
            if(procurementPlanDetail.getProcurementPlan().getBeginDate() != null){
                String format = sdf.format(procurementPlanDetail.getProcurementPlan().getBeginDate());
            }
            if(procurementPlanDetail.getProcurementPlan().getBeginDate() != null){
                String format = sdf.format(procurementPlanDetail.getProcurementPlan().getBeginDate());
                procurementPlanDetail.getProcurementPlan().setTimeTxt(format);
            }
            if(procurementPlanDetail.getProcurementPlan().getEndDate() != null){
                String format2 = sdf.format(procurementPlanDetail.getProcurementPlan().getEndDate());
                procurementPlanDetail.getProcurementPlan().setTimeTxtTwo(format2);
            }
            if(procurementPlanDetail.getProcurementPlan().getArrivalDate() != null){
                String format3 = sdf.format(procurementPlanDetail.getProcurementPlan().getArrivalDate());
                procurementPlanDetail.getProcurementPlan().setTimeTxtThree(format3);
            }
        }
        /* 格式化 响应数据 主要是处理继承了AdviceObject对象的注解@** */
        ObjectFormatterUtils.format(procurementPlanDetail);

        /* 获取审批流程 */
        BpmListProcessLogRequestDTO requestDTO  = new BpmListProcessLogRequestDTO();
        requestDTO.setBusinessId(procurementPlanDetail.getProcurementPlan().getId().toString());
        requestDTO.setProcessId(procurementPlanDetail.getProcurementPlan().getWfProcessId());
        List<BpmListProcessLogResponseDTO> listBpm = bpmService.listProcessLog(requestDTO);
        /* 反转列表 */
        Collections.reverse(listBpm);
//        extractedBpmList(listBpm, dataModel);// 这个还是正常表格那样循环显示
//        dataModel.put("bpmLists", bpmLists);
        List<BpmListProcessLogResponseDTO> newBpmList = new ArrayList<>();
        int mergeCounter = 1;
        for (BpmListProcessLogResponseDTO log : listBpm) {
            // 如果操作名称为 "重新提交"，则插入一条合并行数据
            if ("重新提交".equals(log.getOperateName())) {
                mergeCounter++;
                BpmListProcessLogResponseDTO mergeLog1 = new BpmListProcessLogResponseDTO();
                // 根据你的实际字段，设置一个标识或描述（如合并行）
                mergeLog1.setTaskName("");
                mergeLog1.setOperateName("");
                mergeLog1.setOperateRemark("");
                mergeLog1.setOperateComment("");
                mergeLog1.setStartTime("");
                mergeLog1.setEndTime("");
                mergeLog1.setHandlerName("");
                newBpmList.add(mergeLog1);

                BpmListProcessLogResponseDTO mergeLog2 = new BpmListProcessLogResponseDTO();
                mergeLog2.setTaskName("⚫⚫第" + mergeCounter + "次提交⚫⚫");
                mergeLog2.setOperateName("");
                mergeLog2.setOperateRemark("");
                mergeLog2.setOperateComment("");
                mergeLog2.setStartTime("");
                mergeLog2.setEndTime("");
                mergeLog2.setHandlerName("");
                newBpmList.add(mergeLog2);

                BpmListProcessLogResponseDTO mergeLog3 = new BpmListProcessLogResponseDTO();
                mergeLog3.setTaskName("");
                mergeLog3.setOperateName("");
                mergeLog3.setOperateRemark("");
                mergeLog3.setOperateComment("");
                mergeLog3.setStartTime("");
                mergeLog3.setEndTime("");
                mergeLog3.setHandlerName("");
                newBpmList.add(mergeLog3);
            }
            // 添加原始日志信息
            newBpmList.add(log);
        }
        // 将新列表赋值给 dataModel 对应的 key
        dataModel.put("bpmLists", newBpmList);

        /* 提取操作名称为“提交”的那一条 */
        for (BpmListProcessLogResponseDTO item : listBpm) {
            if ("提交".equals(item.getOperateName())) {
                /* 编制人就是提交人 */
                dataModel.put("bianZhiRen", item.getHandlerName());
                /* 发起日期就是开始时间 */
                dataModel.put("faQiRiQi", item.getStartTime());
                break; // 如果只要第一条，找到就退出
            }
        }

        /*基本信息 */
        dataModel.put("agreement", procurementPlanDetail.getProcurementPlan());
        dataModel.put("contractPlanning", procurementPlanDetail.getContractPlanning());
        /* 清单信息 */
        dataModel.put("splitMaterials", procurementPlanDetail.getSplitMaterials());


        /* 加载模板 */
        ClassPathResource resource = new ClassPathResource(WordAgreementTypeEnum.DEFAULT_PLAN.getPath());
        try (InputStream inputStream = resource.getInputStream()) {

            /* 需要循环的表数据 */
            LoopRowTableRenderPolicy loopRowTableRenderPolicy = new LoopRowTableRenderPolicy();
            Configure config = Configure.builder()
                    .bind("splitMaterials", loopRowTableRenderPolicy)
                    .bind("bpmLists", loopRowTableRenderPolicy)
                    .build();

            XWPFTemplate template = XWPFTemplate.compile(inputStream, config).render(dataModel);
            /* 增加页脚 */
            //extractedAddPageHeaderFooterPlan(template, procurementPlanDetail);

            /* 判断是导出Pdf还是Docx */
            if(type!=null && (type.equals("word") || type.equals("docx") || type.equals("doc"))){
                /* 设置响应头（支持中文文件名） */
                String fileName = URLEncoder.encode(procurementPlanDetail.getProcurementPlan().getProcurementPlanCode()+"采购计划.docx", "UTF-8").replaceAll("\\+", "%20");
                response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
                /* 输出 Word 文件到浏览器 */
                template.writeAndClose(response.getOutputStream());
            }else{
                // 使用本地LibreOffice转换PDF
                ByteArrayOutputStream wordOut = new ByteArrayOutputStream();
                template.writeAndClose(wordOut);

                // 检查LibreOffice是否可用
                if (!LibreOfficeLocalConverter.isLibreOfficeAvailable()) {
                    throw new RuntimeException("LibreOffice未安装或不可用，请先安装LibreOffice");
                }

                // 转换为PDF
                byte[] pdfBytes = LibreOfficeLocalConverter.convertDocxToPdf(wordOut.toByteArray());

                String fileName = URLEncoder.encode(procurementPlanDetail.getProcurementPlan().getProcurementPlanCode() + "采购计划.pdf", "UTF-8").replaceAll("\\+", "%20");
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
                response.getOutputStream().write(pdfBytes);
                wordOut.close();

                /* 适合小型文件存储在内存中，文件大于1MB或者以上内存会有压力。 *//*
                ByteArrayOutputStream wordOut = new ByteArrayOutputStream();
                ByteArrayInputStream wordInput = null;
                ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
                try {
                    template.writeAndClose(wordOut);
                    wordInput = new ByteArrayInputStream(wordOut.toByteArray());
                    convertDocxToPdf(wordInput, pdfOut);
                    *//* 设置 PDF 响应头（下载 PDF） *//*
                    String fileName = URLEncoder.encode(procurementPlanDetail.getProcurementPlan().getProcurementPlanCode() + "采购计划.pdf", "UTF-8").replaceAll("\\+", "%20");
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
                    *//* 输出 PDF 到浏览器 *//*
                    pdfOut.writeTo(response.getOutputStream());
                } finally {
                    if (wordOut != null) wordOut.close();
                    if (wordInput != null) wordInput.close();
                    if (pdfOut != null) pdfOut.close();
                }*/
            }

        }

    }

    private static void convertDocxToPdf(InputStream docxInput, OutputStream pdfOutput) throws Exception {
        XWPFDocument document = new XWPFDocument(docxInput);
        Document pdfDocument = new Document();
        PdfWriter.getInstance(pdfDocument, pdfOutput);
        pdfDocument.open();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null && !text.trim().isEmpty()) {
                pdfDocument.add(new Paragraph(text));
            }
        }

        for (XWPFTable table : document.getTables()) {
            PdfPTable pdfTable = new PdfPTable(table.getRows().get(0).getTableCells().size());
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    pdfTable.addCell(cell.getText());
                }
            }
            pdfDocument.add(pdfTable);
        }
        pdfDocument.close();
        document.close();
    }

    /* 增加页脚 */
    private static void extractedAddPageHeaderFooter(XWPFTemplate template, AgreementDetailVO agreementDetailVO) {
        XWPFDocument document = template.getXWPFDocument();
        XWPFHeaderFooterPolicy policy = document.getHeaderFooterPolicy();

        if (policy == null) {
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            // 设置 footer 边距为 0
            if (sectPr.getPgMar() == null) {
                sectPr.addNewPgMar();
            }
            sectPr.getPgMar().setFooter(BigInteger.ZERO);
            policy = new XWPFHeaderFooterPolicy(document, sectPr);
        }

        /* ========= 设置页眉：页码 ========= */
        XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
        XWPFParagraph headerParagraph = header.createParagraph();
        headerParagraph.setAlignment(ParagraphAlignment.RIGHT);

        XWPFRun run1 = headerParagraph.createRun();
        run1.setText("第 ");
        headerParagraph.getCTP().addNewFldSimple().setInstr("PAGE");

        XWPFRun run2 = headerParagraph.createRun();
        run2.setText(" 页 / 共 ");
        headerParagraph.getCTP().addNewFldSimple().setInstr("NUMPAGES");

        XWPFRun run3 = headerParagraph.createRun();
        run3.setText(" 页");

        /* ========= 设置页脚：链接 ========= */
        XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);

        // 创建一行一列的表格
        XWPFTable table = footer.createTable(1, 1);
        table.setWidth("100%");
        table.getCTTbl().getTblPr().unsetTblBorders(); // 去边框

        XWPFTableCell cell = table.getRow(0).getCell(0);
        XWPFParagraph para = cell.getParagraphs().get(0);
        para.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = para.createRun();
        run.setText("地址：");

        // 构建 Base64 编码链接
        String base64Encoded = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            AgreementBase64VO base64VO = new AgreementBase64VO();
            base64VO.setId(agreementDetailVO.getAgreement().getId().toString());
            base64VO.setType(agreementDetailVO.getAgreement().getExpenditureBusinessType());

            String jsonString = objectMapper.writeValueAsString(base64VO);
            base64Encoded = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
            base64Encoded = URLEncoder.encode(base64Encoded, "UTF-8");
        } catch (Exception e) {
            log.error("[导出合同][Base64编码转换 ERROR] {}", e.getMessage(), e);
        }

        String url = urlAgreement + base64Encoded;

        XWPFHyperlinkRun hyperlinkRun = para.createHyperlinkRun(url);
        hyperlinkRun.setText(url);
        hyperlinkRun.setColor("0000FF");
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
    }

    /* 增加页脚 */
    private static void extractedAddPageHeaderFooterPlan(XWPFTemplate template, ProcurementPlanDetailVO agreementDetailVO) {
        XWPFDocument document = template.getXWPFDocument();
        XWPFHeaderFooterPolicy policy = document.getHeaderFooterPolicy();

        if (policy == null) {
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            // 设置 footer 边距为 0
            if (sectPr.getPgMar() == null) {
                sectPr.addNewPgMar();
            }
            sectPr.getPgMar().setFooter(BigInteger.ZERO);
            policy = new XWPFHeaderFooterPolicy(document, sectPr);
        }

        /* ========= 设置页眉：页码 ========= */
        XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
        XWPFParagraph headerParagraph = header.createParagraph();
        headerParagraph.setAlignment(ParagraphAlignment.RIGHT);

        XWPFRun run1 = headerParagraph.createRun();
        run1.setText("第 ");
        headerParagraph.getCTP().addNewFldSimple().setInstr("PAGE");

        XWPFRun run2 = headerParagraph.createRun();
        run2.setText(" 页 / 共 ");
        headerParagraph.getCTP().addNewFldSimple().setInstr("NUMPAGES");

        XWPFRun run3 = headerParagraph.createRun();
        run3.setText(" 页");

        /* ========= 设置页脚：链接 ========= */
        XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);

        // 创建一行一列的表格
        XWPFTable table = footer.createTable(1, 1);
        table.setWidth("100%");
        table.getCTTbl().getTblPr().unsetTblBorders(); // 去边框

        XWPFTableCell cell = table.getRow(0).getCell(0);
        XWPFParagraph para = cell.getParagraphs().get(0);
        para.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = para.createRun();
        run.setText("地址：");

        // 构建 Base64 编码链接
        String base64Encoded = "";
        try {
            /*ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            AgreementBase64VO base64VO = new AgreementBase64VO();
            base64VO.setId(agreementDetailVO.getProcurementPlan().getId().toString());
            base64VO.setType(agreementDetailVO.getProcurementPlan().getProcurementPlanType());

            String jsonString = objectMapper.writeValueAsString(base64VO);
            base64Encoded = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
            base64Encoded = URLEncoder.encode(base64Encoded, "UTF-8");*/
            base64Encoded = numberToBase64(agreementDetailVO.getProcurementPlan().getId());
        } catch (Exception e) {
            log.error("[导出合同][Base64编码转换 ERROR] {}", e.getMessage(), e);
        }

        String url = urlAgreementPlan + base64Encoded;

        XWPFHyperlinkRun hyperlinkRun = para.createHyperlinkRun(url);
        hyperlinkRun.setText(url);
        hyperlinkRun.setColor("0000FF");
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
    }

    private static String numberToBase64(long number) {
        String numStr = String.valueOf(number);
        byte[] byteData = numStr.getBytes();
        String base64Encoded = Base64.getEncoder().encodeToString(byteData);
        return base64Encoded.replace("=", "");
    }

    /* 重新组装 */
    private Map<String, Object> row(int index, String label , Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("index", index);
        map.put("label", label);
        map.put("value", value);
        return map;
    }

    private void extractedBpmList(List<BpmListProcessLogResponseDTO> listBpm, Map<String, Object> dataModel) {
        List<Map<String, Object>> bpmLists = new ArrayList<>();
        for (int i = 0; i < listBpm.size(); i++) {
            BpmListProcessLogResponseDTO item = listBpm.get(i);
            int index = i + 1;
            bpmLists.add(row(index, "任务名称", item.getTaskName()));
            bpmLists.add(row(index, "前一个处理人ID", item.getPreHandlerId()));
            bpmLists.add(row(index, "前一个处理人名称", item.getPreHandlerName()));
            bpmLists.add(row(index, "当前操作人ID", item.getHandlerId()));
            bpmLists.add(row(index, "当前操作人名称", item.getHandlerName()));
            bpmLists.add(row(index, "操作Code", item.getOperateCode()));
            bpmLists.add(row(index, "操作名称", item.getOperateName()));
            bpmLists.add(row(index, "是否已处理", item.getHandled()));
            bpmLists.add(row(index, "操作备注", item.getOperateRemark()));
            bpmLists.add(row(index, "操作批语", item.getOperateComment()));
            bpmLists.add(row(index, "开始时间", item.getStartTime()));
            bpmLists.add(row(index, "结束时间", item.getEndTime()));
        }
        dataModel.put("bpmLists", bpmLists);
    }

    private void extractedPayment(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementPaymentLists, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementPaymentLists().size(); i++) {
            AgreementPaymentListVO item = agreementDetailVO.getAgreementPaymentLists().get(i);
            int index = i + 1;
            agreementPaymentLists.add(row( index,  "结算阶段",  item.getSettlementStage()));
            agreementPaymentLists.add(row( index,  "付款条件/结算与付款节点",  item.getPaymentName()));
            agreementPaymentLists.add(row( index,  "付款基数（元）",  item.getPaymentBasis()));
            agreementPaymentLists.add(row( index,  "约定付款比例(%)",  item.getPaymentRatio()));
            agreementPaymentLists.add(row( index,  "约定付款金额",  item.getPaymentAmount()));
            agreementPaymentLists.add(row( index,  "付款说明",  item.getPaymentRemark()));
            agreementPaymentLists.add(row( index,  "当前付款节点",  item.getCurrentPaymentPoint()));
        }
        dataModel.put("agreementPaymentLists", agreementPaymentLists);
    }

    private void extractedMaterials(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> materialsList, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getMaterialsList().size(); i++) {
            AgreementMaterialsListVO item = agreementDetailVO.getMaterialsList().get(i);
            int index = i + 1;
            materialsList.add(row( index,  "物料编码",  item.getMaterialsCode()));
            materialsList.add(row( index,  "物料名称",  item.getMaterialsName()));
            materialsList.add(row( index,  "交易标的物编码",  item.getSubjectMatterCode()));
            materialsList.add(row( index,  "交易标的物名称",  item.getSubjectMatterName()));
            materialsList.add(row( index,  "规格型号",  item.getSpecification()));
            materialsList.add(row( index,  "计量单位",  item.getUnitMeasurement()));
            materialsList.add(row( index,  "计量规则",  item.getMeasurementRules()));
            materialsList.add(row( index,  "基本工作内容",  item.getWorkContent()));
            materialsList.add(row( index,  "成本科目",  item.getCostAccount()));
            materialsList.add(row( index,  "发票类型",  item.getBillTypeText()));
            materialsList.add(row( index,  "品牌",  item.getBrand()));
            materialsList.add(row( index,  "价款类型",  item.getPaymentTypeText()));
            materialsList.add(row( index,  "计租单位",  item.getRentalUnitText()));
            materialsList.add(row( index,  "工作量",  item.getWorkload()));
            materialsList.add(row( index,  "租赁方式",  item.getRentModeText()));
            materialsList.add(row( index,  "租赁时间",  item.getRentTimeText()));
            materialsList.add(row( index,  "租赁数量",  item.getRentQuantityText()));
            materialsList.add(row( index,  "中标价 数量",  item.getCountText()));
            materialsList.add(row( index,  "中标价 含税单价(元)",  item.getTaxUnitPriceText()));
            materialsList.add(row( index,  "中标价 不含税单价(元)",  item.getNotTaxUnitPriceText()));
            materialsList.add(row( index,  "中标价 含税总价(元)",  item.getTaxPriceText()));
            materialsList.add(row( index,  "中标价 不含税总价(元)",  item.getNotTaxPriceText()));
            materialsList.add(row( index,  "中标价 税率",  item.getTaxRateText()));
            materialsList.add(row( index,  "中标价 税额",  item.getTaxAmountText()));
            materialsList.add(row( index,  "中标价 浮动价",  item.getFloatingPriceText()));
            materialsList.add(row( index,  "中标价 浮动率",  item.getFloatingRateText()));
            materialsList.add(row( index,  "中标价 卸费",  item.getUnloadingFeeText()));
            materialsList.add(row( index,  "中标价 基价",  item.getBasePriceText()));
            materialsList.add(row( index,  "备注",  item.getRemark()));
/*            materialsList.add(row( index,  "签订 基价",  item.getSignBasePriceText()));
            materialsList.add(row( index,  "签订 浮动价",  item.getSignFloatingPriceText()));
            materialsList.add(row( index,  "签订 浮动率",  item.getSignFloatingRateText()));*/
            materialsList.add(row( index,  "签订 数量",  item.getSignCountText()));
            materialsList.add(row( index,  "签订 税率",  item.getSignTaxRateText()));
            materialsList.add(row( index,  "签订 税率编码",  item.getSignTaxRateCode()));
            materialsList.add(row( index,  "签订 税率名称",  item.getSignTaxRateName()));
            materialsList.add(row( index,  "签订 单价（含税）",  item.getSignUnitPriceInclTaxText()));
            materialsList.add(row( index,  "签订 单价（不含税）",  item.getSignUnitPriceExclTaxText()));
            materialsList.add(row( index,  "签订 金额（含税）",  item.getSignAmountInclTaxText()));
            materialsList.add(row( index,  "签订 金额（不含税）",  item.getSignAmountExclTaxText()));
            materialsList.add(row( index,  "签订 税额",  item.getSignTaxAmountText()));
            materialsList.add(row( index,  "易料市集商品编码",  item.getOfferGoodsCode()));
            materialsList.add(row( index,  "易料市集商品名",  item.getGoodsName()));
            materialsList.add(row( index,  "易料市集品牌",  item.getOfferBrand()));
            materialsList.add(row( index,  "易料市集含税单价",  item.getOfferPrice()));
        }
        dataModel.put("materialsList", materialsList);
    }

    private void extractedDeposits(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementDeposits, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementDeposits().size(); i++) {
            AgreementDepositVO item = agreementDetailVO.getAgreementDeposits().get(i);
            int index = i + 1;
            agreementDeposits.add(row( index,  "押金/保证金类型",  item.getDepositType()));
            agreementDeposits.add(row( index,  "押金/保证金方式",  item.getDepositWay()));
            agreementDeposits.add(row( index,  "保证金类型-文本",  item.getDepositTypeText()));
            agreementDeposits.add(row( index,  "保证金方式-文本",  item.getDepositWayText()));
            agreementDeposits.add(row( index,  "保证金基数",  item.getDepositBaseAmount()));
            agreementDeposits.add(row( index,  "保证金基数（格式化）",  item.getDepositBaseAmountText()));
            agreementDeposits.add(row( index,  "缴纳金额",  item.getPaymentAmount()));
            agreementDeposits.add(row( index,  "缴纳金额（格式化）",  item.getPaymentAmountText()));
            agreementDeposits.add(row( index,  "约定保证金比例",  item.getDepositRatio()));
            agreementDeposits.add(row( index,  "约定保证金比例（格式化）",  item.getDepositRatioText()));
            agreementDeposits.add(row( index,  "约定保证金金额",  item.getDepositAmount()));
            agreementDeposits.add(row( index,  "约定保证金金额（格式化）",  item.getDepositAmountText()));
            agreementDeposits.add(row( index,  "返还条件",  item.getReturnCondition()));
            agreementDeposits.add(row( index,  "备注",  item.getRemark()));
        }
        dataModel.put("agreementDeposits", agreementDeposits);
    }

    private void extractedWage(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementDailyWageList, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementDailyWageList().size(); i++) {
            AgreementDailyWageVO item = agreementDetailVO.getAgreementDailyWageList().get(i);
            int index = i + 1;
            agreementDailyWageList.add(row( index,  "合同编号",  item.getAgreementId()));
            agreementDailyWageList.add(row( index,  "工种名称编码",  item.getJobTitleCode()));
            agreementDailyWageList.add(row( index,  "工种名称",  item.getJobTitleName()));
            agreementDailyWageList.add(row( index,  "计量单位编码",  item.getUnitMeasurementCode()));
            agreementDailyWageList.add(row( index,  "计量单位",  item.getUnitMeasurement()));
            agreementDailyWageList.add(row( index,  "税率",  item.getTaxRate()));
            agreementDailyWageList.add(row( index,  "税率（格式化）",  item.getTaxRateText()));
            agreementDailyWageList.add(row( index,  "单价(不含税)",  item.getUnitPriceExcTax()));
            agreementDailyWageList.add(row( index,  "单价(不含税)-格式化",  item.getUnitPriceExcTaxText()));
            agreementDailyWageList.add(row( index,  "单价(含税)",  item.getUnitPriceIncTax()));
            agreementDailyWageList.add(row( index,  "单价(含税)-格式化",  item.getUnitPriceIncTaxText()));
            agreementDailyWageList.add(row( index,  "备注",  item.getRemark()));
        }
        dataModel.put("agreementDailyWageList", agreementDailyWageList);
    }

    private void extractedShifts(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementMachineShifts, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementMachineShifts().size(); i++) {
            AgreementMachineShiftVO item = agreementDetailVO.getAgreementMachineShifts().get(i);
            int index = i + 1;
            agreementMachineShifts.add(row( index,  "设备名称编码",  item.getEquipmentNameCode()));
            agreementMachineShifts.add(row( index,  "设备名称",  item.getEquipmentName()));
            agreementMachineShifts.add(row( index,  "规格型号",  item.getSpecification()));
            agreementMachineShifts.add(row( index,  "计量单位",  item.getUnitMeasurement()));
            agreementMachineShifts.add(row( index,  "税率",  item.getTaxRate()));
            agreementMachineShifts.add(row( index,  "税率（格式化）",  item.getTaxRateText()));
            agreementMachineShifts.add(row( index,  "单价(不含税)",  item.getUnitPriceExcTax()));
            agreementMachineShifts.add(row( index,  "单价(不含税)-格式化",  item.getUnitPriceExcTaxText()));
            agreementMachineShifts.add(row( index,  "单价(含税)",  item.getUnitPriceIncTax()));
            agreementMachineShifts.add(row( index,  "单价(含税)-格式化",  item.getUnitPriceIncTaxText()));
            agreementMachineShifts.add(row( index,  "备注",  item.getRemark()));
        }
        dataModel.put("agreementMachineShifts", agreementMachineShifts);
    }

    private void extractedEquipment(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementEquipmentSupplies, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementEquipmentSupplies().size(); i++) {
            AgreementEquipmentSupplyVO item = agreementDetailVO.getAgreementEquipmentSupplies().get(i);
            int index = i + 1;
            agreementEquipmentSupplies.add(row( index,  "设备名称编码",  item.getEquipmentNameCode()));
            agreementEquipmentSupplies.add(row( index,  "设备名称",  item.getEquipmentName()));
            agreementEquipmentSupplies.add(row( index,  "规格型号",  item.getSpecification()));
            agreementEquipmentSupplies.add(row( index,  "计量单位",  item.getUnitMeasurement()));
            agreementEquipmentSupplies.add(row( index,  "预估数量",  item.getEstimatedCount()));
            agreementEquipmentSupplies.add(row( index,  "预估数量（格式化）",  item.getEstimatedCountText()));
            agreementEquipmentSupplies.add(row( index,  "预估税率",  item.getEstimatedTaxRate()));
            agreementEquipmentSupplies.add(row( index,  "预估税率（格式化）",  item.getEstimatedTaxRateText()));
            agreementEquipmentSupplies.add(row( index,  "预估单价(不含税)",  item.getEstimatedUnitPriceExcTax()));
            agreementEquipmentSupplies.add(row( index,  "预估单价(不含税)-格式化",  item.getEstimatedUnitPriceExcTaxText()));
            agreementEquipmentSupplies.add(row( index,  "预估单价(含税)",  item.getEstimatedUnitPriceIncTax()));
            agreementEquipmentSupplies.add(row( index,  "预估单价(含税)-格式化",  item.getEstimatedUnitPriceIncTaxText()));
            agreementEquipmentSupplies.add(row( index,  "预估金额(不含税)",  item.getEstimatedAmountExcTax()));
            agreementEquipmentSupplies.add(row( index,  "预估金额(不含税)-格式化",  item.getEstimatedAmountExcTaxText()));
            agreementEquipmentSupplies.add(row( index,  "预估金额(含税)",  item.getEstimatedAmountIncTax()));
            agreementEquipmentSupplies.add(row( index,  "预估金额(含税)-格式化",  item.getEstimatedAmountIncTaxText()));
            agreementEquipmentSupplies.add(row( index,  "预估税额",  item.getEstimatedTaxAmount()));
            agreementEquipmentSupplies.add(row( index,  "预估税额（格式化）",  item.getEstimatedTaxAmountText()));
            agreementEquipmentSupplies.add(row( index,  "备注",  item.getRemark()));
        }
        dataModel.put("agreementEquipmentSupplies", agreementEquipmentSupplies);
    }

    private void extractedAttachment(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementAttachmentList, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementAttachmentList().size(); i++) {
            Attachment item = agreementDetailVO.getAgreementAttachmentList().get(i);
            int index = i + 1;
            agreementAttachmentList.add(row( index,  "文件路径",  item.getFileUrl()));
            agreementAttachmentList.add(row( index,  "文件名",  item.getFileName()));
        }
        dataModel.put("agreementAttachmentList", agreementAttachmentList);
    }

    private void extractedSupplies(AgreementDetailVO agreementDetailVO, List<Map<String, Object>> agreementMaterialSupplies, Map<String, Object> dataModel) {
        for (int i = 0; i < agreementDetailVO.getAgreementMaterialSupplies().size(); i++) {
            AgreementMaterialSupplyVO item = agreementDetailVO.getAgreementMaterialSupplies().get(i);
            int index = i + 1;
            agreementMaterialSupplies.add(row( index,  "物资名称编码",  item.getMaterialNameCode()));
            agreementMaterialSupplies.add(row( index,  "物资名称",  item.getMaterialName()));
            agreementMaterialSupplies.add(row( index,  "规格型号",  item.getSpecification()));
            agreementMaterialSupplies.add(row( index,  "计量单位",  item.getUnitMeasurement()));
            agreementMaterialSupplies.add(row( index,  "预估数量",  item.getEstimatedCount()));
            agreementMaterialSupplies.add(row( index,  "预估数量（格式化）",  item.getEstimatedCountText()));
            agreementMaterialSupplies.add(row( index,  "预估税率",  item.getEstimatedTaxRate()));
            agreementMaterialSupplies.add(row( index,  "预估税率（格式化）",  item.getEstimatedTaxRateText()));
            agreementMaterialSupplies.add(row( index,  "预估单价(不含税)",  item.getEstimatedUnitPriceExcTax()));
            agreementMaterialSupplies.add(row( index,  "预估单价(不含税)-格式化",  item.getEstimatedUnitPriceExcTaxText()));
            agreementMaterialSupplies.add(row( index,  "预估单价(含税)",  item.getEstimatedUnitPriceIncTax()));
            agreementMaterialSupplies.add(row( index,  "预估单价(含税)-格式化",  item.getEstimatedUnitPriceIncTaxText()));
            agreementMaterialSupplies.add(row( index,  "预估金额(不含税)",  item.getEstimatedAmountExcTax()));
            agreementMaterialSupplies.add(row( index,  "预估金额(不含税)-格式化",  item.getEstimatedAmountExcTaxText()));
            agreementMaterialSupplies.add(row( index,  "预估金额(含税)",  item.getEstimatedAmountIncTax()));
            agreementMaterialSupplies.add(row( index,  "预估金额(含税)-格式化",  item.getEstimatedAmountIncTaxText()));
            agreementMaterialSupplies.add(row( index,  "预估税额",  item.getEstimatedTaxAmount()));
            agreementMaterialSupplies.add(row( index,  "预估税额（格式化）",  item.getEstimatedTaxAmountText()));
            agreementMaterialSupplies.add(row( index,  "备注",  item.getRemark()));
        }
        dataModel.put("agreementMaterialSupplies", agreementMaterialSupplies);
    }

}

@Data
class AgreementBase64VO {
    String id;
    Integer type;
}
