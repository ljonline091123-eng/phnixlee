package com.zhaocai.business.pub.utils;

import com.zhaocai.business.agreement.vo.res.AgreementBookmarkVO;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.business.sdk.bean.BookMark;
import com.zhaocai.business.sdk.bean.ConvertParams;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BookmarkUtils {

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private YOZOfileUtils yozOfileUtils;

//    @Autowired
//    private IAgreementService agreementService;

    @Autowired
    private Sender sender;



    //获取文档书签的所有label值
    public String getBookmarkLabel() {
        String type = "document_bookmark";
        List<DictListVO> list = sysDictDataService.listDictByType(type);
        // 提取所有 dictLabel 并组成一个列表
        List<String> labels = list.stream()
                .map(DictListVO::getDictLabel)
                .collect(Collectors.toList());

        // 将标签列表转换为逗号分隔的字符串
        return String.join(",", labels);
    }

    //根据书签名填充数据到文档中
    public String FillBookmarkData(String fileUrl,String fileName, AgreementBookmarkVO agreementBookmarkVO) {
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        ConvertParams params = new ConvertParams();
        // 设置要处理的文档模版
        try {
            params.setFilePath(path.toString());
            params.setAccepTracks(true); //显示修订记录
            List<DictListVO> list = sysDictDataService.listDictByType("document_bookmark");
            BookMark bookMark = new BookMark();

            // 创建一个字段名到getter方法结果(字段值)的映射 Map<key：字段名、value：字段值>
            Map<String, Object> fieldToValueMap = getFieldToValueMap(agreementBookmarkVO);

            for (DictListVO dict : list) {
                // 获取对应字段的值
                Object value = fieldToValueMap.get(dict.getDictValue());
                if (value != null) {
                    // 将值转换为字符串形式，如果需要的话
                    String strValue = value.toString();
                    bookMark.addBookMarkInfo(dict.getDictLabel(), strValue, BookMark.BOOKMARK_TYPE_TXT, BookMark.BOOKMARK_OTYPE_REPLACE);
                }
            }

            params.setBookMark(bookMark);
            // 提交处理文档
            String response = sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
            System.out.println("转换文件响应结果：");
            System.out.println("response:" + response);
            String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
//            String newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
            System.out.println("填充书签后生成的文件下载地址:" + viewUrl);
            return viewUrl;
        } catch (Exception e) {
            throw new RuntimeException("书签位置填充数据失败," + e.getMessage(), e);
        }
    }

    //获取字段名到getter方法结果(字段值)的映射 Map<key：字段名、value：字段值>
    private Map<String, Object> getFieldToValueMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
//        使用 obj.getClass() 获取传入对象的 Class 对象
        Class<?> clazz = obj.getClass();
//        调用 getDeclaredFields() 来获取该类声明的所有字段，包括私有字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(obj));
        }
        return map;
    }

    //接受修订记录，不显示线下修订记录
    public String AcceptanceAmendmentRecord(String fileUrl,String fileName) {
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        ConvertParams params = new ConvertParams();
        // 设置要处理的文档模版
        try {
            params.setFilePath(path.toString());
            params.setAccepTracks(false);   //显示修订记录

            // 提交处理文档
            String response = sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_CONVERT_DOCUMENT, params.getRequestBody());
            System.out.println("转换文件响应结果：");
            System.out.println("response:" + response);
            String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
            System.out.println("填充书签后生成的文件下载地址:" + viewUrl);
            return viewUrl;
        } catch (Exception e) {
            throw new RuntimeException("书签位置填充数据失败," + e.getMessage(), e);
        }
        finally {
            //删除生成的临时文件
            System.out.println("删除文件路径:" + path);
            yozOfileUtils.deleteTempFilePath(path.toString());
        }
    }
}
