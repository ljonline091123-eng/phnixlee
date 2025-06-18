package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.domain.LaborServicesClassExcelData;

import java.util.List;

/**
 * 劳务分类主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILaborServicesClassService extends IService<LaborServicesClass> {
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
     * 批量删除劳务分类主
     *
     * @param ids 需要删除的劳务分类主主键集合
     * @return 结果
     */
    public boolean deleteLaborServicesClassByIds(String[] ids);

    /**
     * 删除劳务分类主信息
     *
     * @param id 劳务分类主主键
     * @return 结果
     */
    public int deleteLaborServicesClassById(String id);

    List<LabourTypeTree> getLaborServicesClassTree();

    LaborServicesClass initCode(LaborServicesClass laborServicesClass);

    long selectLaborServicesClassListCount(LaborServicesClass laborServicesClass);

    String importData(List<LaborServicesClassExcelData> userList, boolean updateSupport, String operName);
}
