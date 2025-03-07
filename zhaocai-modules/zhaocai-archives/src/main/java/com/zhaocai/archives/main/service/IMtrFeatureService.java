package com.zhaocai.archives.main.service;

import java.util.List;

import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 材料特征项主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMtrFeatureService  extends IService<MtrFeature>
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
     * 批量删除材料特征项主
     *
     * @param ids 需要删除的材料特征项主主键集合
     * @return 结果
     */
    public boolean deleteMtrFeatureByIds(String[] ids);

    /**
     * 删除材料特征项主信息
     *
     * @param id 材料特征项主主键
     * @return 结果
     */
    public int deleteMtrFeatureById(String id);

    MtrFeature initCode(MtrClass mtrClass);

    long selectMtrFeatureListCount(MtrFeature mtrFeature);
}
