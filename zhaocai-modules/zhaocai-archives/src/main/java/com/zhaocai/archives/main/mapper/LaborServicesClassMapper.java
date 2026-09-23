package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 劳务分类主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LaborServicesClassMapper extends BaseMapper<LaborServicesClass>
{
    /**
     * 查询劳务分类主
     *
     * @param id 劳务分类主主键
     * @return 劳务分类主
     */
    public LaborServicesClass selectLaborServicesClassById(String id);

    /**
     * 查询劳务分类主列表
     *
     * @param laborServicesClass 劳务分类主
     * @return 劳务分类主集合
     */
    public List<LaborServicesClass> selectLaborServicesClassList(LaborServicesClass laborServicesClass);

    /**
     * 新增劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    public int insertLaborServicesClass(LaborServicesClass laborServicesClass);

    /**
     * 修改劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    public int updateLaborServicesClass(LaborServicesClass laborServicesClass);

    /**
     * 删除劳务分类主
     *
     * @param id 劳务分类主主键
     * @return 结果
     */
    public int deleteLaborServicesClassById(String id);

    /**
     * 批量删除劳务分类主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLaborServicesClassByIds(String[] ids);

    public Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") String upId);

    long selectLaborServicesClassListCount(LaborServicesClass laborServicesClass);

    int getMaterialJoinNoMy(String id);
}
