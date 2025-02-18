package com.zhaocai.business.pub.service;

import com.zhaocai.business.pub.vo.res.DictListVO;

import java.util.List;

/**
 * 字典数据Service接口
 *
 * @author WH
 * @date 2024-06-03
 */
public interface ISysDictDataService {

    /**
     * 获取字典值
     * @param type
     * @param value
     * @return
     */
    String getLabel(String type, String value);

    /**
     * 获取业务字典
     * @param type
     * @return
     */
    List<DictListVO> listDictByType(String type);

    /**
     * 获取字典值
     * @param type
     * @param value
     * @return
     */
    String getRemark(String type, String value,String classVaule);
}
