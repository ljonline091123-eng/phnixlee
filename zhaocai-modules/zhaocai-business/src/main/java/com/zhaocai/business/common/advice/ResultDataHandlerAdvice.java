package com.zhaocai.business.common.advice;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.reflect.ReflectUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

/**
 * 响应结果统一处理
 *
 * @author chenming
 * @date 2024/05/31
 */
@Slf4j
@ControllerAdvice
public class ResultDataHandlerAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 仅处理ResultData
        if (returnType.getParameterType().equals(ResultData.class)){
            String packageName = returnType.getContainingClass().getPackage().getName();
            return packageName.startsWith("com.zhaocai.business");
        }
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        ResultData<?> resultData = (ResultData<?>) body;
        if (resultData != null && resultData.isSuccess() && resultData.getData() != null) {
            // 只处理请求成功的情况
            Object data = resultData.getData();
            if (isPrimitiveOrWrapper(data.getClass())) {
                // 基础类型直接返回
                return resultData;
            } else if(data instanceof List<?>){
                // 处理 List
                handleResult4List((List<?>) data);
            } else if (data instanceof PageResult) {
                // 处理 分页
                List<?> dataList = ((PageResult<?>) data).getRows();
                handleResult4List(dataList);
            } else {
                handleResult4Object(data);
            }
        }
        return resultData;
    }

    /**
     * 处理 List 集合
     * @param dataList
     */
    private void handleResult4List(List<?> dataList) {
        for (Object data : dataList) {
            handleResult4Object(data);
        }
    }

    /**
     * 处理每一个对象
     * @param obj
     */
    private void handleResult4Object(Object obj) {
        // 1. 不为 null
        // 2. 不为基础类型
        // 3. 必须是 AdviceObject 的子类
        if (obj != null
                && !isPrimitiveOrWrapper(obj.getClass())
                && AdviceObject.class.isAssignableFrom(obj.getClass())) {
            doHandleResult4Object(obj);
        }

    }

    /**
     * 处理对象
     * @param obj
     */
    private void doHandleResult4Object(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DictCache.class)) {
                // 处理被 DictBizEnum 注解标注的字段
                processDictBizEnumField(obj, field);
            } else if (field.isAnnotationPresent(MoneyFormat.class)) {
                // 处理金额格式化
                processMoneyFormatField(obj,field);
            } else {
                try{
                    Object fieldValue = field.get(obj);
                    // 基础类行不处理
                    if (fieldValue != null && !isPrimitiveOrWrapper(fieldValue.getClass())) {
                        if (fieldValue instanceof List<?>) {
                            handleResult4List((List<?>) fieldValue);
                        } else {
                            handleResult4Object(fieldValue);
                        }
                    }
                } catch (Exception e) {
                    log.error("执行 ResultDataHandlerAdvice 失败，cause by:{}",e.getMessage(),e);
                }
            }
        }
    }

    /**
     * 金额格式化
     * @param obj
     * @param field
     */
    private void processMoneyFormatField(Object obj, Field field) {
        MoneyFormat moneyFormat = field.getAnnotation(MoneyFormat.class);
        String valueFieldName = moneyFormat.filedName();
        int scale = moneyFormat.scale();
        if (StringUtils.isNotBlank(valueFieldName)) {
            BigDecimal amountValue = ReflectUtils.getFieldValue(obj,valueFieldName);
            if (amountValue != null) {
                String value = NumberUtil.decimalFormat(amountValue,scale);
                ReflectUtils.setFieldValue(obj,field.getName(),value);
            }
        }
    }

    /**
     * 处理缓存枚举
     * @param obj
     * @param field
     */
    private void processDictBizEnumField(Object obj, Field field) {
        DictCache dictCache = field.getAnnotation(DictCache.class);

        DictBizEnum dictBizEnum = dictCache.dictBizEnum();
        String filedName = dictCache.filedName();
        if (dictBizEnum != null && StringUtils.isNotBlank(filedName)) {
            Object fileValue = ReflectUtils.getFieldValue(obj,filedName);
            if (fileValue != null) {
                String label = DictBizCache.getValue(dictBizEnum,fileValue.toString());
                ReflectUtils.setFieldValue(obj,field.getName(),label);
            }
        }
    }

    /**
     * 判断是否为基础类型
     * @param type
     * @return
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Integer.class ||
                type == Long.class ||
                type == Double.class ||
                type == Float.class ||
                type == Boolean.class ||
                type == Character.class ||
                type == Byte.class ||
                type == Short.class ||
                type == Void.class;
    }
}
