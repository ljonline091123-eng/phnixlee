package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.LaborServicesFeature;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 劳务特征项主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LaborServicesFeatureMapper extends BaseMapper<LaborServicesFeature>
{
    /**
     * 查询劳务特征项主
     *
     * @param id 劳务特征项主主键
     * @return 劳务特征项主
     */
    public LaborServicesFeature selectLaborServicesFeatureById(String id);

    /**
     * 查询劳务特征项主列表
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 劳务特征项主集合
     */
    public List<LaborServicesFeature> selectLaborServicesFeatureList(LaborServicesFeature laborServicesFeature);

    /**
     * 新增劳务特征项主
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 结果
     */
    public int insertLaborServicesFeature(LaborServicesFeature laborServicesFeature);

    /**
     * 修改劳务特征项主
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 结果
     */
    public int updateLaborServicesFeature(LaborServicesFeature laborServicesFeature);

    /**
     * 删除劳务特征项主
     *
     * @param id 劳务特征项主主键
     * @return 结果
     */
    public int deleteLaborServicesFeatureById(String id);

    /**
     * 批量删除劳务特征项主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLaborServicesFeatureByIds(String[] ids);

    long selectLaborServicesFeatureListCount(LaborServicesFeature laborServicesFeature);
}
