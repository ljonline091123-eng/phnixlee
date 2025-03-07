package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 专业分包分类主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MajorSubcontractingClassMapper extends BaseMapper<MajorSubcontractingClass>
{
    /**
     * 查询专业分包分类主
     *
     * @param id 专业分包分类主主键
     * @return 专业分包分类主
     */
    public MajorSubcontractingClass selectMajorSubcontractingClassById(String id);

    /**
     * 查询专业分包分类主列表
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 专业分包分类主集合
     */
    public List<MajorSubcontractingClass> selectMajorSubcontractingClassList(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 新增专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    public int insertMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 修改专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    public int updateMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 删除专业分包分类主
     *
     * @param id 专业分包分类主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingClassById(String id);

    /**
     * 批量删除专业分包分类主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMajorSubcontractingClassByIds(String[] ids);

    public Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") String upId);

    long selectMajorSubcontractingClassListCount(MajorSubcontractingClass majorSubcontractingClass);

    int getMaterialJoinNoMy(String id);
}
