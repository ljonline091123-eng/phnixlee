package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;

import java.util.List;

/**
 * 专业分包特征值主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMajorSubcontractingFeatureValueService extends IService<MajorSubcontractingFeatureValue> {
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
     * 批量删除专业分包特征值主
     *
     * @param ids 需要删除的专业分包特征值主主键集合
     * @return 结果
     */
    public boolean deleteMajorSubcontractingFeatureValueByIds(String[] ids);

    /**
     * 删除专业分包特征值主信息
     *
     * @param id 专业分包特征值主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingFeatureValueById(String id);

    MajorSubcontractingFeatureValue initCode(MajorSubcontractingFeature mtrClass);

    long selectMajorSubcontractingFeatureValueListCount(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue);
}
