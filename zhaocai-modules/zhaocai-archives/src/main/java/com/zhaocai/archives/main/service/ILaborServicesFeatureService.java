package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.LaborServicesFeature;

import java.util.List;

/**
 * 劳务特征项主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILaborServicesFeatureService extends IService<LaborServicesFeature> {
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
     * 批量删除劳务特征项主
     *
     * @param ids 需要删除的劳务特征项主主键集合
     * @return 结果
     */
    public boolean deleteLaborServicesFeatureByIds(String[] ids);

    /**
     * 删除劳务特征项主信息
     *
     * @param id 劳务特征项主主键
     * @return 结果
     */
    public int deleteLaborServicesFeatureById(String id);

    long selectLaborServicesFeatureListCount(LaborServicesFeature laborServicesFeature);
}
