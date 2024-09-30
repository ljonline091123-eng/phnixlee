package com.zhaocai.common.core.utils.bean;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ssy
 * @date 2024/5/27 11:10
 */
public class BeanCopierUtil {

    /**
     * BeanCopier的缓存
     */
    static final ConcurrentHashMap<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    /**
     * copy source 对象
     * @param source
     * @param clasz
     * @param <T>
     * @return
     */
    public static <T extends Object> T copyBean(Object source,Class<T> clasz) {
        Object target = null;
        try {
            target = clasz.newInstance();
            if (source != null) {
                copyBean(source,target);
            }
        } catch (Exception e) {
            throw new RuntimeException("copy bean 失败",e);
        }

        return (T) target;
    }

    /**
     * 将 source 对象 copy 到 target 对象
     * @param source
     * @param target
     */
    public static void copyBean(Object source, Object target) {
        String key = genKey(source.getClass(), target.getClass());
        BeanCopier beanCopier;
        if (BEAN_COPIER_CACHE.containsKey(key)) {
            beanCopier = BEAN_COPIER_CACHE.get(key);
        } else {
            beanCopier = BeanCopier.create(source.getClass(), target.getClass(), false);
            BEAN_COPIER_CACHE.put(key, beanCopier);
        }

        beanCopier.copy(source, target, null);
    }

    /**
     * copy list
     * @param sourceList
     * @param clasz
     * @param <T>
     * @return
     */
    public static <T extends Object> List<T> copyList(List<?> sourceList , Class<T> clasz) {
        List<T> resultList = new ArrayList<>();

        if (CollectionUtils.isEmpty(sourceList)) {
            return resultList;
        }

        for (Object source : sourceList) {
            T target = copyBean(source,clasz);

            resultList.add(target);
        }

        return resultList;
    }

    /**
     * 生成key
     * @param srcClazz 源文件的class
     * @param tgtClazz 目标文件的class
     * @return string
     */
    private static String genKey(Class<?> srcClazz, Class<?> tgtClazz) {
        return srcClazz.getName() + tgtClazz.getName();
    }
}
