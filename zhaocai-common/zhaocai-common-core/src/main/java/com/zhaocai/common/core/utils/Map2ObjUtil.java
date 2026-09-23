package com.zhaocai.common.core.utils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * map转Obj
 * @author ssy
 * @date 2024/7/11 20:22
 */
public class Map2ObjUtil {

    public static <T> T convertToObject(LinkedHashMap<String, Object> map, Class<T> clazz) {
        T instance;
        try {
            instance = clazz.newInstance();
            for (String key : map.keySet()) {
                try {
                    Field field = clazz.getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(instance, map.get(key));
                } catch (NoSuchFieldException e) {
                    // 忽略不存在的属性
                    // 也可以选择记录日志或抛出异常
                    System.out.println("Ignoring non-existing field: " + key);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instance;
    }

}
