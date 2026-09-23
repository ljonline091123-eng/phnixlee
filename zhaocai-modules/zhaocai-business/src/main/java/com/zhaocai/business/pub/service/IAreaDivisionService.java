package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.vo.res.AreaDivisionTreeVO;
import com.zhaocai.business.pub.vo.res.AreaDivisionVO;

import java.util.List;
import java.util.Map;

/**
 * 行政区划Service接口
 *
 * @author WH
 * @date 2024-07-12
 */
public interface IAreaDivisionService extends IService<AreaDivision> {

    Boolean deleteSyncAreaDivision();

    /**
     * 根据 parentCode 获取行政区划
     * @param parentCode
     * @return
     */
    List<AreaDivisionVO> listAreaDivisionByParentCode(String parentCode);

    /**
     * 根据行政区划编码获取行政区划
     * @param areaCode
     * @return
     */
    AreaDivision getByCode(String areaCode);

    /**
     * 获取省市树形结构
     * @return
     */
    List<AreaDivisionTreeVO> listAreaDivisionTree();

    /**
     * 获取行政区域
     * @param codeList
     * @return
     */
    Map<String, String> getAreaDivisionMap(List<String> codeList);
}
