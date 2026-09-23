package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.LabourItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;

/**
 * 劳务特征项Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LabourItemMapper extends BaseMapper<LabourItem>
{
    /**
     * 查询劳务特征项
     *
     * @param id 劳务特征项主键
     * @return 劳务特征项
     */
    public LabourItem selectLabourItemById(Long id);

    /**
     * 查询劳务特征项列表
     *
     * @param labourItem 劳务特征项
     * @return 劳务特征项集合
     */
    public List<LabourItem> selectLabourItemList(LabourItem labourItem);

    public long selectLabourItemListCount(LabourItem labourItem);

    /**
     * 新增劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    public int insertLabourItem(LabourItem labourItem);

    /**
     * 修改劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    public int updateLabourItem(LabourItem labourItem);

    /**
     * 删除劳务特征项
     *
     * @param id 劳务特征项主键
     * @return 结果
     */
    public int deleteLabourItemById(Long id);

    /**
     * 批量删除劳务特征项
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabourItemByIds(Long[] ids);


    List<LabourItem> getProcessed(String organCode);
}
