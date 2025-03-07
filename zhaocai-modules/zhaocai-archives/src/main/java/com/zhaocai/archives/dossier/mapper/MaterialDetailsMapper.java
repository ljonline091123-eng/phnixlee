package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialItem;

/**
 * 材料详情Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MaterialDetailsMapper extends BaseMapper<MaterialDetails>
{
    /**
     * 查询材料详情
     *
     * @param id 材料详情主键
     * @return 材料详情
     */
    public MaterialDetails selectMaterialDetailsById(Long id);

    /**
     * 查询材料详情列表
     *
     * @param materialDetails 材料详情
     * @return 材料详情集合
     */
    public List<MaterialDetails> selectMaterialDetailsList(MaterialDetails materialDetails);

    public long selectMaterialDetailsListCount(MaterialDetails materialDetails);

    /**
     * 新增材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    public int insertMaterialDetails(MaterialDetails materialDetails);

    /**
     * 修改材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    public int updateMaterialDetails(MaterialDetails materialDetails);

    /**
     * 删除材料详情
     *
     * @param id 材料详情主键
     * @return 结果
     */
    public int deleteMaterialDetailsById(Long id);

    /**
     * 批量删除材料详情
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMaterialDetailsByIds(Long[] ids);


    List<MaterialDetails> getProcessed(String organCode);

}
