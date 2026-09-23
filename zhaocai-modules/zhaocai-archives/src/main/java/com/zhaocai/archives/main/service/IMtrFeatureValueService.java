package com.zhaocai.archives.main.service;

import java.util.List;

import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 材料特征值主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMtrFeatureValueService  extends IService<MtrFeatureValue>
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
     * 批量删除材料特征值主
     *
     * @param ids 需要删除的材料特征值主主键集合
     * @return 结果
     */
    public boolean deleteMtrFeatureValueByIds(String[] ids);

    /**
     * 删除材料特征值主信息
     *
     * @param id 材料特征值主主键
     * @return 结果
     */
    public int deleteMtrFeatureValueById(String id);

    MtrFeatureValue initCode(MtrFeature mtrClass);

    long selectMtrFeatureValueListCount(MtrFeatureValue mtrFeatureValue);
}
