package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 材料特征值主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MtrFeatureValueMapper extends BaseMapper<MtrFeatureValue>
{
    /**
     * 查询材料特征值主
     *
     * @param id 材料特征值主主键
     * @return 材料特征值主
     */
    public MtrFeatureValue selectMtrFeatureValueById(String id);

    /**
     * 查询材料特征值主列表
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 材料特征值主集合
     */
    public List<MtrFeatureValue> selectMtrFeatureValueList(MtrFeatureValue mtrFeatureValue);

    /**
     * 新增材料特征值主
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 结果
     */
    public int insertMtrFeatureValue(MtrFeatureValue mtrFeatureValue);

    /**
     * 修改材料特征值主
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 结果
     */
    public int updateMtrFeatureValue(MtrFeatureValue mtrFeatureValue);

    /**
     * 删除材料特征值主
     *
     * @param id 材料特征值主主键
     * @return 结果
     */
    public int deleteMtrFeatureValueById(String id);

    /**
     * 批量删除材料特征值主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMtrFeatureValueByIds(String[] ids);

    String getMaxCode(String id);

    long selectMtrFeatureValueListCount(MtrFeatureValue mtrFeatureValue);
}
