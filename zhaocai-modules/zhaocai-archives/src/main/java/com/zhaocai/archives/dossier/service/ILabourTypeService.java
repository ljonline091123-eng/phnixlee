package com.zhaocai.archives.dossier.service;

import java.util.List;
import java.util.Map;

import com.zhaocai.archives.dossier.domain.LabourType;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.LaborServicesClass;

/**
 * 劳务分类Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILabourTypeService  extends IService<LabourType>
{
    /**
     * 查询劳务分类
     *
     * @param id 劳务分类主键
     * @return 劳务分类
     */
    public LabourType selectLabourTypeById(Long id);

    public LabourType selectLabourTypeByIdNoChange(Long id);

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
     * 批量删除劳务分类
     *
     * @param ids 需要删除的劳务分类主键集合
     * @return 结果
     */
    public boolean deleteLabourTypeByIds(Long[] ids);

    /**
     * 删除劳务分类信息
     *
     * @param id 劳务分类主键
     * @return 结果
     */
    public int deleteLabourTypeById(Long id);

    List<LabourType> initData(LabourType labourType);

    LabourType initMaterialType(LabourType labourType);

    List<LabourTypeTree> getLabourTypeTree(LabourType labourType);


    void addTypeByMain(LaborServicesClass mtrClass);

    void updateByHostId(LaborServicesClass mtrClass);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    void processAuditPass(Map<String, Object> variables);


    int addToMain(LabourType labourType);

    int associationToMain(LabourType labourType);

    int unAssociationToMain(LabourType labourType);


    long selectLabourTypeListCount(LabourType labourType);

    int getMaterialJoin(Long id);

    int updateLabourTypeNoChange(LabourType materialType);
}
