package com.zhaocai.archives.dossier.mapper;

import java.util.List;

import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 专业分包特征项Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface SubcontractingItemMapper extends BaseMapper<SubcontractingItem>
{
    /**
     * 查询专业分包特征项
     *
     * @param id 专业分包特征项主键
     * @return 专业分包特征项
     */
    public SubcontractingItem selectSubcontractingItemById(Long id);

    /**
     * 查询专业分包特征项列表
     *
     * @param subcontractingItem 专业分包特征项
     * @return 专业分包特征项集合
     */
    public List<SubcontractingItem> selectSubcontractingItemList(SubcontractingItem subcontractingItem);

    public long selectSubcontractingItemListCount(SubcontractingItem subcontractingItem);

    /**
     * 新增专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    public int insertSubcontractingItem(SubcontractingItem subcontractingItem);

    /**
     * 修改专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    public int updateSubcontractingItem(SubcontractingItem subcontractingItem);

    /**
     * 删除专业分包特征项
     *
     * @param id 专业分包特征项主键
     * @return 结果
     */
    public int deleteSubcontractingItemById(Long id);

    /**
     * 批量删除专业分包特征项
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSubcontractingItemByIds(Long[] ids);


    List<SubcontractingItem> getProcessed(String organCode);
}
