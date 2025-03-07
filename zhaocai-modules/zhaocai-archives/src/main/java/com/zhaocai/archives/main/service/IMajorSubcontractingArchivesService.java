package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;

import java.util.List;

/**
 * 专业分包档案主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMajorSubcontractingArchivesService extends IService<MajorSubcontractingArchives> {
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
     * 批量删除专业分包档案主
     *
     * @param ids 需要删除的专业分包档案主主键集合
     * @return 结果
     */
    public boolean deleteMajorSubcontractingArchivesByIds(String[] ids);

    /**
     * 删除专业分包档案主信息
     *
     * @param id 专业分包档案主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingArchivesById(String id);


    MajorSubcontractingArchives initDetails(MajorSubcontractingArchives majorSubcontractingArchives);

    long selectMajorSubcontractingArchivesListCount(MajorSubcontractingArchives majorSubcontractingArchives);
}
