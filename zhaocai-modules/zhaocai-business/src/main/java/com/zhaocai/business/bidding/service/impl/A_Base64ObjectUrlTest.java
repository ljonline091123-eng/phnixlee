package com.zhaocai.business.bidding.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.procurement.vo.res.BiddingSchemeListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeTemplateVO;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * 对象base64转码测试
 */
public class A_Base64ObjectUrlTest {

    public static void main(String[] args) throws UnsupportedEncodingException, JsonProcessingException, ParseException {
        // 日期格式解析器
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 示例数据 JSON 字符串
        String data = "{\n" +
                "    \"id\": \"1872978024622239745\",\n" +
                "    \"procurementSchemeCode\": \"CGRW202400000636\",\n" +
                "    \"procurementSchemeName\": \"主体大劳务110\",\n" +
                "    \"procurementPlanType\": 5,\n" +
                "    \"procurementPlanTypeText\": \"劳务分包\",\n" +
                "    \"procurementType\": 1,\n" +
                "    \"procurementTypeText\": \"公开招标\",\n" +
                "    \"procurementOfficerName\": \"刘芳\",\n" +
                "    \"createTime\": \"2024-12-28\",\n" +
                "    \"state\": 3,\n" +
                "    \"stateText\": \"审批通过\",\n" +
                "    \"biddingTemplate\": {\n" +
                "        \"attachmentId\": \"1872977896893100033\",\n" +
                "        \"fileName\": \"JKTY_ddbe877bc8784679b7b59d8b498a3116_20241228200823565.docx\",\n" +
                "        \"fileUrl\": \"https://zc.hncig.cn:32068/minio/wh-hnjt/JKTY_ddbe877bc8784679b7b59d8b498a3116_20241228200823565.docx\"\n" +
                "    },\n" +
                "    \"bidContactPerson\": \"刘永\",\n" +
                "    \"bidContactPhone\": \"12222212311\",\n" +
                "    \"bidContactEmail\": \"1112@qq.com\",\n" +
                "    \"projectCode\": \"SG20012088000008-1\",\n" +
                "    \"bidDeadline\": \"2025-01-04 20:08:00\",\n" +
                "    \"createId\": \"1846994203039801361\",\n" +
                "    \"purchaseOfficer\": false\n" +
                "}";


        Date createTime = dateFormat.parse("2024-12-28");
        Date bidDeadline = dateTimeFormat.parse("2025-01-04 20:08:00");
        // 创建 BiddingSchemeListVO 对象并设置属性
        BiddingSchemeListVO biddingSchemeListVO = getBiddingSchemeListVO(createTime, bidDeadline);

        // 创建 ObjectMapper 并配置忽略 null 值
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        // 将对象序列化为 JSON 字符串
        String jsonString = objectMapper.writeValueAsString(biddingSchemeListVO);
        System.out.println("对象序列化数据:");
        System.out.println(jsonString);

        // 使用 Base64 编码 JSON 字符串
        String base64Encoded = Base64.getEncoder().encodeToString(jsonString.getBytes("UTF-8"));
        System.out.println("Base64 编码后的 JSON 数据:");
        System.out.println(base64Encoded);

        // 模仿 encodeURIComponent
        String encoded = URLEncoder.encode(base64Encoded, "UTF-8");
        System.out.println("URLEncoder 编码:");
        System.out.println(encoded);

        // 解密 Base64 编码的字符串
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        String decodedString = new String(decodedBytes, "UTF-8");
        System.out.println("解密 Base64 编码的字符串");
        System.out.println(decodedString);



    }

    private static @NotNull BiddingSchemeListVO getBiddingSchemeListVO(Date createTime, Date bidDeadline) {
        BiddingSchemeListVO biddingSchemeListVO = new BiddingSchemeListVO();
        biddingSchemeListVO.setCreateTime(createTime);
        biddingSchemeListVO.setBidDeadline(bidDeadline);
        biddingSchemeListVO.setId(1872978024622239745L);
        biddingSchemeListVO.setProcurementSchemeCode("CGRW202400000636");
        biddingSchemeListVO.setProcurementSchemeName("主体大劳务110");
        biddingSchemeListVO.setProcurementPlanType(5);
        biddingSchemeListVO.setProcurementPlanTypeText("劳务分包");
        biddingSchemeListVO.setProcurementType(1);
        biddingSchemeListVO.setProcurementTypeText("公开招标");
        biddingSchemeListVO.setProcurementOfficerName("刘芳");
        biddingSchemeListVO.setState(3);
        biddingSchemeListVO.setStateText("审批通过");
        biddingSchemeListVO.setBidContactPerson("刘永");
        biddingSchemeListVO.setBidContactPhone("12222212311");
        biddingSchemeListVO.setBidContactEmail("1112@qq.com");
        biddingSchemeListVO.setProjectCode("SG20012088000008-1");
        biddingSchemeListVO.setCreateId(1846994203039801361L);
        biddingSchemeListVO.setPurchaseOfficer(false);
        // 创建 ProcurementSchemeTemplateVO 对象并设置模板
        ProcurementSchemeTemplateVO schemeTemplateVO = new ProcurementSchemeTemplateVO(
                1872977896893100033L,
                "JKTY_ddbe877bc8784679b7b59d8b498a3116_20241228200823565.docx",
                "https://zc.hncig.cn:32068/minio/wh-hnjt/JKTY_ddbe877bc8784679b7b59d8b498a3116_20241228200823565.docx"
        );
        biddingSchemeListVO.setBiddingTemplate(schemeTemplateVO);
        return biddingSchemeListVO;
    }


}
