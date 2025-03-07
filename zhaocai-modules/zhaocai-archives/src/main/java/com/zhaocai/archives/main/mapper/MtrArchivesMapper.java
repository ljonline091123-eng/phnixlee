package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.MtrArchives;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 材料档案主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MtrArchivesMapper extends BaseMapper<MtrArchives>
{
    /**
     * 查询材料档案主
     *
     * @param id 材料档案主主键
     * @return 材料档案主
     */
    public MtrArchives selectMtrArchivesById(String id);

    /**
     * 查询材料档案主列表
     *
     * @param mtrArchives 材料档案主
     * @return 材料档案主集合
     */
    public List<MtrArchives> selectMtrArchivesList(MtrArchives mtrArchives);

    /**
     * 新增材料档案主
     *
     * @param mtrArchives 材料档案主
     * @return 结果
     */
    public int insertMtrArchives(MtrArchives mtrArchives);

    /**
     * 修改材料档案主
     *
     * @param mtrArchives 材料档案主
     * @return 结果
     */
    public int updateMtrArchives(MtrArchives mtrArchives);

    /**
     * 删除材料档案主
     *
     * @param id 材料档案主主键
     * @return 结果
     */
    public int deleteMtrArchivesById(String id);

    /**
     * 批量删除材料档案主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMtrArchivesByIds(String[] ids);

    String getMaxCode(String id);

    long selectMtrArchivesListCount(MtrArchives mtrArchives);
}
