package com.zhaocai.business.common.cache;


import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.common.core.utils.SpringUtils;

/**
 * 业务字典缓存工具类
 *
 * @author chenming
 * @date 2024/06/03
 */
public class DictBizCache {
    private static final String DICT_ID = "dictBiz";

    private static ISysDictDataService sysDictDataService;

    static {
        sysDictDataService = SpringUtils.getBean(ISysDictDataService.class);
    }

    /**
     * 获取字典值
     * @param code
     * @param dictKey
     * @return
     */
    public static String getValue(DictBizEnum code, String dictKey) {
        //todo 增加缓存 key
        return getValue(code.getName(), dictKey);
    }

    /**
     * 获取字典值
     * @param name
     * @param dictKey
     * @return
     */
    private static String getValue(String name, String dictKey) {
        //todo 增加缓存 key
        String cacheKey = DICT_ID.concat(":").concat(name);
        return sysDictDataService.getLabel(name,dictKey);
    }
}
