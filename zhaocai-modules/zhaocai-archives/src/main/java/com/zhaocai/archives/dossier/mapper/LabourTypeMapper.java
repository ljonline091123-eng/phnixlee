package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.LabourType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;
import org.apache.ibatis.annotations.Param;

/**
 * 劳务分类Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LabourTypeMapper extends BaseMapper<LabourType>
{
    /**
     * 查询劳务分类
     *
     * @param id 劳务分类主键
     * @return 劳务分类
     */
    public LabourType selectLabourTypeById(Long id);

    /**
     * 查询劳务分类列表
     *
     * @param labourType 劳务分类
     * @return 劳务分类集合
     */
    public List<LabourType> selectLabourTypeList(LabourType labourType);

    /**
     * 新增劳务分类
     *
     * @param labourType 劳务分类
     * @return 结果
     */
    public int insertLabourType(LabourType labourType);

    /**
     * 修改劳务分类
     *
     * @param labourType 劳务分类
     * @return 结果
     */
    public int updateLabourType(LabourType labourType);

    /**
     * 删除劳务分类
     *
     * @param id 劳务分类主键
     * @return 结果
     */
    public int deleteLabourTypeById(Long id);

    /**
     * 批量删除劳务分类
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabourTypeByIds(Long[] ids);

    Integer getMaxCode(@Param("upCode") String upCode,@Param("upId") Long upId,@Param("organCode") String organCode);

    long selectLabourTypeListCount(LabourType labourType);

    int getMaterialJoinNoMy(Long id);

    int getMaterialJoin(Long id);

    List<LabourType> getProcessed(String organCode);


    Integer getReuseTowCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode, @Param("pdz") Integer pdz);
}
