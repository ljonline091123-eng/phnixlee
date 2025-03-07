package com.zhaocai.archives.main.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.main.domain.LaborServicesArchives;
import org.apache.ibatis.annotations.Param;

/**
 * 劳务档案主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LaborServicesArchivesMapper extends BaseMapper<LaborServicesArchives>
{
    /**
     * 查询劳务档案主
     *
     * @param id 劳务档案主主键
     * @return 劳务档案主
     */
    public LaborServicesArchives selectLaborServicesArchivesById(String id);

    /**
     * 查询劳务档案主列表
     *
     * @param laborServicesArchives 劳务档案主
     * @return 劳务档案主集合
     */
    public List<LaborServicesArchives> selectLaborServicesArchivesList(LaborServicesArchives laborServicesArchives);

    /**
     * 新增劳务档案主
     *
     * @param laborServicesArchives 劳务档案主
     * @return 结果
     */
    public int insertLaborServicesArchives(LaborServicesArchives laborServicesArchives);

    /**
     * 修改劳务档案主
     *
     * @param laborServicesArchives 劳务档案主
     * @return 结果
     */
    public int updateLaborServicesArchives(LaborServicesArchives laborServicesArchives);

    /**
     * 删除劳务档案主
     *
     * @param id 劳务档案主主键
     * @return 结果
     */
    public int deleteLaborServicesArchivesById(String id);

    /**
     * 批量删除劳务档案主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLaborServicesArchivesByIds(String[] ids);

    Integer getMaxCode(@Param("typeId") String classId, @Param("oldCode") String code);

    long selectLaborServicesArchivesListCount(LaborServicesArchives laborServicesArchives);
}
