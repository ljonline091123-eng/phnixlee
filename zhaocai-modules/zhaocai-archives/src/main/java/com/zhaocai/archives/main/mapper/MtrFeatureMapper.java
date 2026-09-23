package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 材料特征项主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MtrFeatureMapper extends BaseMapper<MtrFeature>
{
    /**
     * 查询材料特征项主
     *
     * @param id 材料特征项主主键
     * @return 材料特征项主
     */
    public MtrFeature selectMtrFeatureById(String id);

    /**
     * 查询材料特征项主列表
     *
     * @param mtrFeature 材料特征项主
     * @return 材料特征项主集合
     */
    public List<MtrFeature> selectMtrFeatureList(MtrFeature mtrFeature);

    /**
     * 新增材料特征项主
     *
     * @param mtrFeature 材料特征项主
     * @return 结果
     */
    public int insertMtrFeature(MtrFeature mtrFeature);

    /**
     * 修改材料特征项主
     *
     * @param mtrFeature 材料特征项主
     * @return 结果
     */
    public int updateMtrFeature(MtrFeature mtrFeature);

    /**
     * 删除材料特征项主
     *
     * @param id 材料特征项主主键
     * @return 结果
     */
    public int deleteMtrFeatureById(String id);

    /**
     * 批量删除材料特征项主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMtrFeatureByIds(String[] ids);

    String getMaxCode(String id);

    long selectMtrFeatureListCount(MtrFeature mtrFeature);
}
