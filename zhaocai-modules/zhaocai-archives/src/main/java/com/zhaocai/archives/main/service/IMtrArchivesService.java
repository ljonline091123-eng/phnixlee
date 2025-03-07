package com.zhaocai.archives.main.service;

import java.util.List;
import com.zhaocai.archives.main.domain.MtrArchives;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MtrClass;

/**
 * 材料档案主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMtrArchivesService  extends IService<MtrArchives>
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
     * 批量删除材料档案主
     *
     * @param ids 需要删除的材料档案主主键集合
     * @return 结果
     */
    public boolean deleteMtrArchivesByIds(String[] ids);

    /**
     * 删除材料档案主信息
     *
     * @param id 材料档案主主键
     * @return 结果
     */
    public int deleteMtrArchivesById(String id);

    MtrArchives initCode(MtrClass mtrClass);

    long selectMtrArchivesListCount(MtrArchives mtrArchives);
}
