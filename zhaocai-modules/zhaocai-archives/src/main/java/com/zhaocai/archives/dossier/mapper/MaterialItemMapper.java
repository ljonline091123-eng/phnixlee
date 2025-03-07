package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialItem;

import java.util.List;

/**
 * 材料特征项Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MaterialItemMapper extends BaseMapper<MaterialItem> {
    /**
     * 查询材料特征项
     *
     * @param id 材料特征项主键
     * @return 材料特征项
     */
    public MaterialItem selectMaterialItemById(Long id);

    /**
     * 查询材料特征项列表
     *
     * @param materialItem 材料特征项
     * @return 材料特征项集合
     */
    public List<MaterialItem> selectMaterialItemList(MaterialItem materialItem);

    public long selectMaterialItemListCount(MaterialItem materialItem);

    /**
     * 新增材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    public int insertMaterialItem(MaterialItem materialItem);

    /**
     * 修改材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    public int updateMaterialItem(MaterialItem materialItem);

    /**
     * 删除材料特征项
     *
     * @param id 材料特征项主键
     * @return 结果
     */
    public int deleteMaterialItemById(Long id);

    /**
     * 批量删除材料特征项
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMaterialItemByIds(Long[] ids);

    List<MaterialItem> getProcessed(String organCode);
}
