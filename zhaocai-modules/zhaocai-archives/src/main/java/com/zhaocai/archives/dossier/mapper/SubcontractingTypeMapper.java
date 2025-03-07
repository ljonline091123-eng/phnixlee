package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业分包分类Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface SubcontractingTypeMapper extends BaseMapper<SubcontractingType> {
    /**
     * 查询专业分包分类
     *
     * @param id 专业分包分类主键
     * @return 专业分包分类
     */
    public SubcontractingType selectSubcontractingTypeById(Long id);

    /**
     * 查询专业分包分类列表
     *
     * @param subcontractingType 专业分包分类
     * @return 专业分包分类集合
     */
    public List<SubcontractingType> selectSubcontractingTypeList(SubcontractingType subcontractingType);

    /**
     * 新增专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    public int insertSubcontractingType(SubcontractingType subcontractingType);

    /**
     * 修改专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    public int updateSubcontractingType(SubcontractingType subcontractingType);

    /**
     * 删除专业分包分类
     *
     * @param id 专业分包分类主键
     * @return 结果
     */
    public int deleteSubcontractingTypeById(Long id);

    /**
     * 批量删除专业分包分类
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSubcontractingTypeByIds(Long[] ids);

    Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode);

    long selectSubcontractingTypeListCount(SubcontractingType subcontractingType);

    int getMaterialJoinNoMy(Long id);

    int getMaterialJoin(Long id);


    List<SubcontractingType> getProcessed(String organCode);


    Integer getReuseTowCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode, @Param("pdz") Integer pdz);
}
