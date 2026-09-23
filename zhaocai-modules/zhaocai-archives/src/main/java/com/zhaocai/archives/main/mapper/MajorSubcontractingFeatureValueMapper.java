package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 专业分包特征值主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MajorSubcontractingFeatureValueMapper extends BaseMapper<MajorSubcontractingFeatureValue>
{
    /**
     * 查询专业分包特征值主
     *
     * @param id 专业分包特征值主主键
     * @return 专业分包特征值主
     */
    public MajorSubcontractingFeatureValue selectMajorSubcontractingFeatureValueById(String id);

    /**
     * 查询专业分包特征值主列表
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 专业分包特征值主集合
     */
    public List<MajorSubcontractingFeatureValue> selectMajorSubcontractingFeatureValueList(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue);

    /**
     * 新增专业分包特征值主
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 结果
     */
    public int insertMajorSubcontractingFeatureValue(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue);

    /**
     * 修改专业分包特征值主
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 结果
     */
    public int updateMajorSubcontractingFeatureValue(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue);

    /**
     * 删除专业分包特征值主
     *
     * @param id 专业分包特征值主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureValueById(String id);

    /**
     * 批量删除专业分包特征值主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureValueByIds(String[] ids);

    String getMaxCode(String id);

    long selectMajorSubcontractingFeatureValueListCount(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue);
}
