package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.LaborServicesArchives;

import java.util.List;

/**
 * 劳务档案主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILaborServicesArchivesService extends IService<LaborServicesArchives> {
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
     * 批量删除劳务档案主
     *
     * @param ids 需要删除的劳务档案主主键集合
     * @return 结果
     */
    public boolean deleteLaborServicesArchivesByIds(String[] ids);

    /**
     * 删除劳务档案主信息
     *
     * @param id 劳务档案主主键
     * @return 结果
     */
    public int deleteLaborServicesArchivesById(String id);


    LaborServicesArchives initDetails(LaborServicesArchives laborServicesArchives);

    long selectLaborServicesArchivesListCount(LaborServicesArchives laborServicesArchives);
}
