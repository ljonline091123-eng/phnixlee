package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 专业分包特征项主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MajorSubcontractingFeatureMapper extends BaseMapper<MajorSubcontractingFeature>
{
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
     * 删除专业分包特征项主
     *
     * @param id 专业分包特征项主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureById(String id);

    /**
     * 批量删除专业分包特征项主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureByIds(String[] ids);

    String getMaxCode(String id);

    long selectMajorSubcontractingFeatureListCount(MajorSubcontractingFeature majorSubcontractingFeature);
}
