package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 材料分类Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MaterialTypeMapper extends BaseMapper<MaterialType> {
    /**
     * 查询材料分类
     *
     * @param id 材料分类主键
     * @return 材料分类
     */
    public MaterialType selectMaterialTypeById(Long id);

    /**
     * 查询材料分类列表
     *
     * @param materialType 材料分类
     * @return 材料分类集合
     */
    public List<MaterialType> selectMaterialTypeList(MaterialType materialType);

    /**
     * 新增材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    public int insertMaterialType(MaterialType materialType);

    /**
     * 修改材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    public int updateMaterialType(MaterialType materialType);

    /**
     * 删除材料分类
     *
     * @param id 材料分类主键
     * @return 结果
     */
    public int deleteMaterialTypeById(Long id);

    /**
     * 批量删除材料分类
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMaterialTypeByIds(Long[] ids);

    int getMaterialJoin(Long id);

    int getMaterialJoinNoMy(Long id);

    Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode);

    long selectMaterialTypeListCount(MaterialType materialType);

    List<MaterialType> getProcessed(String organCode);

    Integer getReuseTowCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode, @Param("pdz") Integer pdz);
}
