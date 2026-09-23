package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.LaborServicesFeatureValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 劳务特征值主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LaborServicesFeatureValueMapper extends BaseMapper<LaborServicesFeatureValue>
{
    /**
     * 查询劳务特征值主
     *
     * @param id 劳务特征值主主键
     * @return 劳务特征值主
     */
    public LaborServicesFeatureValue selectLaborServicesFeatureValueById(String id);

    /**
     * 查询劳务特征值主列表
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 劳务特征值主集合
     */
    public List<LaborServicesFeatureValue> selectLaborServicesFeatureValueList(LaborServicesFeatureValue laborServicesFeatureValue);

    /**
     * 新增劳务特征值主
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 结果
     */
    public int insertLaborServicesFeatureValue(LaborServicesFeatureValue laborServicesFeatureValue);

    /**
     * 修改劳务特征值主
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 结果
     */
    public int updateLaborServicesFeatureValue(LaborServicesFeatureValue laborServicesFeatureValue);

    /**
     * 删除劳务特征值主
     *
     * @param id 劳务特征值主主键
     * @return 结果
     */
    public int deleteLaborServicesFeatureValueById(String id);

    /**
     * 批量删除劳务特征值主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLaborServicesFeatureValueByIds(String[] ids);

    long selectLaborServicesFeatureValueListCount(LaborServicesFeatureValue laborServicesFeatureValue);
}
