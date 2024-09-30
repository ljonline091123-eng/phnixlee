package com.zhaocai.common.core.utils;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ssy
 * @date 2024/8/22 19:34
 */
public class ListUtil {

    public static <T> List<List<T>> splitList(List<T> list, int listSize){
        if (CollectionUtils.isEmpty(list) || listSize == 0 ) {
            return new ArrayList<>();
        }
        int length = list.size();
        int num = ( length + listSize - 1 )/listSize;
        List<List<T>> newList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            int fromIndex = i * listSize;
            int toIndex = Math.min((i + 1) * listSize, length);
            newList.add(list.subList(fromIndex, toIndex)) ;
        }
        return  newList ;
    }

}
