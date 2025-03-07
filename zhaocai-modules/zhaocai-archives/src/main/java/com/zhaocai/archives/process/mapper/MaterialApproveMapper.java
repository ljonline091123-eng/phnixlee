package com.zhaocai.archives.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.process.domain.MaterialApprove;

import java.util.List;

/**
 * 物料审批辅Mapper接口
 *
 * @author lzq
 * @date 2025-02-12
 */
public interface MaterialApproveMapper extends BaseMapper<MaterialApprove> {
    /**
     * 查询物料审批辅
     *
     * @param id 物料审批辅主键
     * @return 物料审批辅
     */
    public MaterialApprove selectMaterialApproveById(Long id);

    /**
     * 查询物料审批辅列表
     *
     * @param materialApprove 物料审批辅
     * @return 物料审批辅集合
     */
    public List<MaterialApprove> selectMaterialApproveList(MaterialApprove materialApprove);

    /**
     * 新增物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    public int insertMaterialApprove(MaterialApprove materialApprove);

    /**
     * 修改物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    public int updateMaterialApprove(MaterialApprove materialApprove);

    /**
     * 删除物料审批辅
     *
     * @param id 物料审批辅主键
     * @return 结果
     */
    public int deleteMaterialApproveById(Long id);

    /**
     * 批量删除物料审批辅
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMaterialApproveByIds(Long[] ids);
}
