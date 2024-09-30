package com.zhaocai.business.common.utils;

import com.zhaocai.business.agreement.vo.res.AgreementContactVO;
import com.zhaocai.business.agreement.vo.res.AgreementWordInfoVO;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.common.core.utils.StringUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AgreementWordUtils {

    public static void replaceFileKeyWord(InputStream inputStream, ByteArrayOutputStream bos, AgreementWordInfoVO agreementWordInfo,String fileName) {
        Map<String, String> replacements = new HashedMap();
        replacements.put("$合同编号$",agreementWordInfo.getAgreementCode());
        replacements.put("$采购方$",agreementWordInfo.getPartyAName());
        replacements.put("$项目名称$",agreementWordInfo.getPartyBName());

        replacements.put("$含税总额大写$",agreementWordInfo.getTotalAmountIncTaxText());
        replacements.put("$含税总额$",agreementWordInfo.getTotalAmountIncTax().toString());
        replacements.put("$不含税总额大写$",agreementWordInfo.getTotalAmountExcTaxText());
        replacements.put("$不含税总额$",agreementWordInfo.getTotalAmountExcTax().toString());

        replacements.put("$甲方联系人$",agreementWordInfo.getPartyAContact().getContactName());
        replacements.put("$甲方联系人电话$",agreementWordInfo.getPartyAContact().getContactPhone());
        replacements.put("$甲方联系人身份证$",agreementWordInfo.getPartyAContact().getContactIdCard());

        replacements.put("$乙方联系人$",agreementWordInfo.getPartyBContact().getContactName());
        replacements.put("$乙方联系人电话$",agreementWordInfo.getPartyBContact().getContactPhone());
        replacements.put("$乙方联系人身份证$",agreementWordInfo.getPartyBContact().getContactIdCard());

        replacements.put("$乙方发票抬头$",agreementWordInfo.getVendor().getEnterpriseName());
        replacements.put("$乙方纳税人识别号$",agreementWordInfo.getVendor().getSocialCreditCode());
        replacements.put("$乙方地址$",agreementWordInfo.getVendor().getEnterpriseAddress());
        replacements.put("$乙方电话$",agreementWordInfo.getVendor().getContactPhone());
        replacements.put("$乙方开户行$",agreementWordInfo.getVendor().getAccountBranch());
        replacements.put("$乙方账号$",agreementWordInfo.getVendor().getBankAccount());
        replacements.put("$乙方开票金额$",agreementWordInfo.getTotalAmountExcTax().toString());

        List<List<String>> listQuotations = getListQuotations(agreementWordInfo.getBiddingListQuotationList());


        // 替换段落中的字符串
        if(fileName.endsWith(".doc")) {
            replaceDocInParagraph(inputStream,bos,replacements);
        } else if (fileName.endsWith(".docx")) {
            replaceDocxInParagraph(inputStream,bos,replacements,listQuotations);
        }
    }

    private static List<List<String>> getListQuotations(List<VendorBiddingListQuotationListVO> biddingListQuotationList) {
        List<List<String>> resultList = new ArrayList<>();
        resultList.add(Arrays.asList("产品名称","规格型号","计量单位","数量","不含税单价（元）","不含税合价（元）","含税单价（元）","含税合价（元）"));
        for(VendorBiddingListQuotationListVO listVO : biddingListQuotationList) {
            resultList.add(Arrays.asList(listVO.getMaterialsName(),listVO.getSpecification(),listVO.getUnitMeasurement(),String.valueOf(listVO.getCount()),
                            String.valueOf(listVO.getTaxUnitPrice()),String.valueOf(listVO.getTaxPrice()),String.valueOf(listVO.getNotTaxUnitPrice()),String.valueOf(listVO.getNotTaxPrice())));
        }

        return resultList;
    }

    /**
     * 替換 docx 的文件內容
     * @param inputStream
     * @param bos
     * @param replacements
     */
    private static void replaceDocInParagraph(InputStream inputStream, ByteArrayOutputStream bos,Map<String, String> replacements) {
        try(HWPFDocument document = new HWPFDocument(inputStream)) {
            // 遍历段落并替换关键词
            Range range = document.getRange();

            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                range.replaceText(entry.getKey(), entry.getValue());
            }

            // 替換表中內容
            // 替换表格内容
            TableIterator tableIterator = new TableIterator(range);
            while (tableIterator.hasNext()) {
                Table table = tableIterator.next();
                for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
                    TableRow row = table.getRow(rowIndex);
                    for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                        TableCell cell = row.getCell(cellIndex);
                        for (int paraIndex = 0; paraIndex < cell.numParagraphs(); paraIndex++) {
                            String text = cell.getParagraph(paraIndex).text();
                            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                if (text.contains(entry.getKey()) && StringUtils.isNoneBlank(entry.getKey()) &&
                                        StringUtils.isNotBlank(entry.getValue())) {
                                    text = text.replace(entry.getKey(), entry.getValue());
                                    cell.getParagraph(paraIndex).replaceText(text, false);
                                }
                            }
                        }
                    }
                }
            }
            // 写入到输出流中
            document.write(bos);
        }catch (Exception e) {
            throw new BusinessException(ResultCode.FAILURE,"生成合同文件失败",e);
        }
    }

    /**
     * 替換 docx 的文件內容
     * @param inputStream
     * @param bos
     * @param replacements
     */
    private static void replaceDocxInParagraph(InputStream inputStream, ByteArrayOutputStream bos,Map<String, String> replacements,List<List<String>> listQuotations) {
        try(XWPFDocument document = new XWPFDocument(inputStream)) {
            // 遍历段落并替换关键词
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replaceInParagraph(paragraph, replacements);
            }

            // 替换表格中的内容
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                String text = r.getText(0);
                                if (text != null) {
                                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                         if (text.contains(entry.getKey()) && StringUtils.isNoneBlank(entry.getKey())
                                                 && StringUtils.isNoneBlank(entry.getValue())) {
                                            text = text.replace(entry.getKey(), entry.getValue());
                                            r.setText(text, 0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 写入物料清单
            XWPFTable table = document.createTable(listQuotations.size(),listQuotations.get(0).size());
            for (int row = 0; row < listQuotations.size(); row++) {
                XWPFTableRow tableRow = table.getRow(row);
                for (int col = 0; col < listQuotations.get(row).size(); col++) {
                    XWPFTableCell cell = tableRow.getCell(col);
                    cell.setText(listQuotations.get(row).get(col));
                }
            }
            document.setTable(0,table);

            // 写入到输出流中
            document.write(bos);
        }catch (Exception e) {
            throw new BusinessException(ResultCode.FAILURE,"生成合同文件失败",e);
        }
    }

    private static void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    if (text.contains(entry.getKey()) && StringUtils.isNoneBlank(entry.getKey()) &&
                            StringUtils.isNoneBlank(entry.getValue())) {
                        text = text.replace(entry.getKey(), entry.getValue());
                    }
                }
                run.setText(text, 0);
            }
        }
    }


    public static void main(String[] args) throws Exception {

        System.out.println(URLConnection.guessContentTypeFromName("材料采购合同通用范本.docx"));

        AgreementWordInfoVO agreementWordInfo = new AgreementWordInfoVO();
        agreementWordInfo.setAgreementCode("CGHT-2024-100001");
        agreementWordInfo.setPartyAName("湖南建工集团");
        agreementWordInfo.setPartyBName("湖南狗蛋科技有限公司");
        agreementWordInfo.setBelongOrganizationName("湖南德必拆迁项目");

        agreementWordInfo.setTotalAmountIncTaxText("捌拾玖亿伍仟肆佰柒拾捌万玖仟陆佰零壹元贰角贰分");
        agreementWordInfo.setTotalAmountIncTax(new BigDecimal("8954789601.22"));
        agreementWordInfo.setTotalAmountExcTaxText("捌拾玖亿伍仟叁佰陆拾伍万捌仟柒佰肆拾伍元捌角玖分");
        agreementWordInfo.setTotalAmountExcTax(new BigDecimal("8953658745.89"));

        Vendor vendor = new Vendor();
        vendor.setEnterpriseName("湖南狗蛋科技有限公司");
        vendor.setSocialCreditCode("23442363534635443253245");
        vendor.setEnterpriseAddress("湖南省长沙市岳麓区");
        vendor.setContactPhone("13670041458");
        vendor.setAccountBranch("长沙银行岳麓区雷锋大道支行");
        vendor.setBankAccount("6224 8878 8858 7899 445");
        agreementWordInfo.setVendor(vendor);

        AgreementContactVO contactVO = new AgreementContactVO();
        contactVO.setContactName("李霞");
        contactVO.setContactPhone("13588789585");
        contactVO.setContactIdCard("469875128954784589");
        agreementWordInfo.setPartyAContact(contactVO);

        contactVO = new AgreementContactVO();
        contactVO.setContactName("王磊");
        contactVO.setContactPhone("15878852569");
        contactVO.setContactIdCard("456325878965411252");
        agreementWordInfo.setPartyBContact(contactVO);

        List<VendorBiddingListQuotationListVO> biddingListQuotationList = new ArrayList<>();
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        biddingListQuotationList.add(buildVendorBiddingListQuotation("抗震III级螺纹钢","10以内（含10mm)","顿",new BigDecimal(10),new BigDecimal(3864),new BigDecimal("997"),new BigDecimal("2345"),new BigDecimal("9876")));
        agreementWordInfo.setBiddingListQuotationList(biddingListQuotationList);
        InputStream inputStream = new FileInputStream(new File("D:\\wh-workspace\\wh-document\\招采平台\\湖南建投\\合同范本\\材料采购合同通用范本.docx"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        replaceFileKeyWord(inputStream,outputStream,agreementWordInfo,"材料采购合同通用范本.docx");

        FileOutputStream fileOutputStream = new FileOutputStream("D:\\wh-workspace\\wh-document\\招采平台\\湖南建投\\合同范本\\材料采购合同-11.docx");
        fileOutputStream.write(outputStream.toByteArray());

        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
        IOUtils.closeQuietly(fileOutputStream);
    }

    private static VendorBiddingListQuotationListVO buildVendorBiddingListQuotation(String materialsName,String specification,String unitMeasurement,
            BigDecimal count,BigDecimal taxUnitPrice,BigDecimal notTaxUnitPrice,BigDecimal taxPrice,BigDecimal notTaxPrice) {
        VendorBiddingListQuotationListVO listVO = new VendorBiddingListQuotationListVO();
        listVO.setMaterialsName(materialsName);
        listVO.setSpecification(specification);
        listVO.setUnitMeasurement(unitMeasurement);
        listVO.setCount(count);
        listVO.setTaxUnitPrice(taxUnitPrice);
        listVO.setNotTaxUnitPrice(notTaxUnitPrice);
        listVO.setTaxPrice(taxPrice);
        listVO.setNotTaxPrice(notTaxPrice);
        return listVO;
    }
}
