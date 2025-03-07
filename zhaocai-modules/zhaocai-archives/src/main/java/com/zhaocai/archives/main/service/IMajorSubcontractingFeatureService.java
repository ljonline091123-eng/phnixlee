package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;

import java.util.List;

/**
 * 专业分包特征项主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMajorSubcontractingFeatureService extends IService<MajorSubcontractingFeature> {
    /**
     * 查询专业分包特征项主
     *
     * @param id 专业分包特征项主主键
     * @return 专业分包特征项主
     */
    public MajorSubcontractingFeature selectMajorSubcontractingFeatureById(String id);

    /**
     * 查询专业分包特征项主列表
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 专业分包特征项主集合
     */
    public List<MajorSubcontractingFeature> selectMajorSubcontractingFeatureList(MajorSubcontractingFeature majorSubcontractingFeature);

    /**
     * 新增专业分包特征项主
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 结果
     */
    public int insertMajorSubcontractingFeature(MajorSubcontractingFeature majorSubcontractingFeature);

    /**
     * 修改专业分包特征项主
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 结果
     */
    public int updateMajorSubcontractingFeature(MajorSubcontractingFeature majorSubcontractingFeature);

    /**
     * 批量删除专业分包特征项主
     *
     * @param ids 需要删除的专业分包特征项主主键集合
     * @return 结果
     */
    public boolean deleteMajorSubcontractingFeatureByIds(String[] ids);

    /**
     * 删除专业分包特征项主信息
     *
     * @param id 专业分包特征项主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureById(String id);

    MajorSubcontractingFeature initCode(MajorSubcontractingClass mtrClass);

    long selectMajorSubcontractingFeatureListCount(MajorSubcontractingFeature majorSubcontractingFeature);
}
