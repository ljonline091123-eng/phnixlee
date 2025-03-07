package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 专业分包档案主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MajorSubcontractingArchivesMapper extends BaseMapper<MajorSubcontractingArchives>
{
    /**
     * 查询专业分包档案主
     *
     * @param id 专业分包档案主主键
     * @return 专业分包档案主
     */
    public MajorSubcontractingArchives selectMajorSubcontractingArchivesById(String id);

    /**
     * 查询专业分包档案主列表
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 专业分包档案主集合
     */
    public List<MajorSubcontractingArchives> selectMajorSubcontractingArchivesList(MajorSubcontractingArchives majorSubcontractingArchives);

    /**
     * 新增专业分包档案主
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 结果
     */
    public int insertMajorSubcontractingArchives(MajorSubcontractingArchives majorSubcontractingArchives);

    /**
     * 修改专业分包档案主
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 结果
     */
    public int updateMajorSubcontractingArchives(MajorSubcontractingArchives majorSubcontractingArchives);

    /**
     * 删除专业分包档案主
     *
     * @param id 专业分包档案主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingArchivesById(String id);

    /**
     * 批量删除专业分包档案主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMajorSubcontractingArchivesByIds(String[] ids);


    Integer getMaxCode(@Param("typeId") String majorSubcontractingClassId,@Param("oldCode") String majorSubcontractingClassCode);

    long selectMajorSubcontractingArchivesListCount(MajorSubcontractingArchives majorSubcontractingArchives);
}
