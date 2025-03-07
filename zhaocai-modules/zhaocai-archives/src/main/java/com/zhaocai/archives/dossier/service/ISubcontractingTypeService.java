package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;

import java.util.List;
import java.util.Map;

/**
 * 专业分包分类Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ISubcontractingTypeService extends IService<SubcontractingType> {
    /**
     * 查询专业分包分类
     *
     * @param id 专业分包分类主键
     * @return 专业分包分类
     */
    public SubcontractingType selectSubcontractingTypeById(Long id);


    public SubcontractingType selectSubcontractingTypeByIdNoChange(Long id);

    /**
     * 查询专业分包分类列表
     *
     * @param subcontractingType 专业分包分类
     * @return 专业分包分类集合
     */
    public List<SubcontractingType> selectSubcontractingTypeList(SubcontractingType subcontractingType);

    /**
     * 新增专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    public int insertSubcontractingType(SubcontractingType subcontractingType);

    /**
     * 修改专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    public int updateSubcontractingType(SubcontractingType subcontractingType);

    /**
     * 批量删除专业分包分类
     *
     * @param ids 需要删除的专业分包分类主键集合
     * @return 结果
     */
    public boolean deleteSubcontractingTypeByIds(Long[] ids);

    /**
     * 删除专业分包分类信息
     *
     * @param id 专业分包分类主键
     * @return 结果
     */
    public int deleteSubcontractingTypeById(Long id);

    List<SubcontractingTypeTree> getSubcontractingTypeTree(SubcontractingType subcontractingType);

    List<SubcontractingType> initData(SubcontractingType subcontractingType);

    SubcontractingType initSubcontractingType(SubcontractingType subcontractingType);

    void addTypeByMain(MajorSubcontractingClass mtrClass);

    void updateByHostId(MajorSubcontractingClass mtrClass);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    void processAuditPass(Map<String, Object> variables);

    int addToMain(SubcontractingType subcontractingType);

    int associationToMain(SubcontractingType subcontractingType);

    int unAssociationToMain(SubcontractingType subcontractingType);


    long selectSubcontractingTypeListCount(SubcontractingType subcontractingType);

    int getMaterialJoin(Long id);

    int updateSubcontractingTypeNoChange(SubcontractingType materialType);
}
