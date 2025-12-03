package com.zhaocai.business.common.utils;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.reflect.ReflectUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/* 格式化工具类 */
public class ObjectFormatterUtils {

    public static void format(Object obj) {
        if (obj == null) return;

        if (obj instanceof List<?>) {
            for (Object item : (List<?>) obj) {
                formatObject(item);
            }
        } else {
            formatObject(obj);
        }
    }

    private static void formatObject(Object obj) {
        if (obj == null || isPrimitiveOrWrapper(obj.getClass())) return;

        if (!(obj instanceof AdviceObject)) return;

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(DictCache.class)) {
                processDictField(obj, field);
            } else if (field.isAnnotationPresent(MoneyFormat.class)) {
                processMoneyField(obj, field);
            } else {
                try {
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && !isPrimitiveOrWrapper(fieldValue.getClass())) {
                        if (fieldValue instanceof List<?>) {
                            format(fieldValue);
                        } else {
                            formatObject(fieldValue);
                        }
                    }
                } catch (Exception e) {
                    // 可选：添加日志
                }
            }
        }
    }

    private static void processMoneyField(Object obj, Field field) {
        MoneyFormat moneyFormat = field.getAnnotation(MoneyFormat.class);
        String valueFieldName = moneyFormat.filedName();
        int scale = moneyFormat.scale();
        if (StringUtils.isNotBlank(valueFieldName)) {
            BigDecimal amountValue = ReflectUtils.getFieldValue(obj, valueFieldName);
            if (amountValue != null) {
                String value = NumberUtil.decimalFormat(amountValue, scale);
                ReflectUtils.setFieldValue(obj, field.getName(), value);
            }
        }
    }

    private static void processDictField(Object obj, Field field) {
        DictCache dictCache = field.getAnnotation(DictCache.class);
        DictBizEnum dictBizEnum = dictCache.dictBizEnum();
        String valueFieldName = dictCache.filedName();
        if (dictBizEnum != null && StringUtils.isNotBlank(valueFieldName)) {
            Object codeValue = ReflectUtils.getFieldValue(obj, valueFieldName);
            if (codeValue != null) {
                String label = DictBizCache.getValue(dictBizEnum, codeValue.toString());
                ReflectUtils.setFieldValue(obj, field.getName(), label);
            }
        }
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Integer.class || type == Long.class ||
                type == Double.class || type == Float.class ||
                type == Boolean.class || type == Character.class ||
                type == Byte.class || type == Short.class || type == Void.class ||
                type == String.class || type == BigDecimal.class || type == Date.class;
    }
}
