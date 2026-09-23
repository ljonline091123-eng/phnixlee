package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.MtrClass;

import java.util.List;
import java.util.Map;

/**
 * 材料分类Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMaterialTypeService extends IService<MaterialType> {
    /**
     * 查询材料分类
     *
     * @param id 材料分类主键
     * @return 材料分类
     */
    public MaterialType selectMaterialTypeById(Long id);

    public MaterialType selectMaterialTypeByIdNoChange(Long id);

    /**
     * 查询材料分类列表
     *
     * @param materialType 材料分类
     * @return 材料分类集合
     */
    public List<MaterialType> selectMaterialTypeList(MaterialType materialType);


    public List<MaterialType> initData(MaterialType materialType);

    /**
     * 新增材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    public int insertMaterialType(MaterialType materialType);

    /**
     * 修改材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    public int updateMaterialType(MaterialType materialType);

    /**
     * 批量删除材料分类
     *
     * @param ids 需要删除的材料分类主键集合
     * @return 结果
     */
    public boolean deleteMaterialTypeByIds(Long[] ids);

    /**
     * 删除材料分类信息
     *
     * @param id 材料分类主键
     * @return 结果
     */
    public int deleteMaterialTypeById(Long id);

    /**
     * 获取材料分类树
     *
     * @param materialType
     * @return
     */
    List<MaterialTypeTree> getMaterialTypeTree(MaterialType materialType);

    MaterialType initMaterialType(MaterialType materialType);

    void updateByHostId(MtrClass mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    void addMaterialTypeByMain(MtrClass mtrClass);

    List<MaterialTypeTree> getDeptTree();

    int addToMain(MaterialType materialType);

    int associationToMain(MaterialType materialType);

    int unAssociationToMain(MaterialType materialType);

    Map<String, String> getSecondaryUnit(String organCode);

    long selectMaterialTypeListCount(MaterialType materialType);

    int getMaterialJoin(Long id);

    void updateMaterialTypeNoChange(MaterialType materialType);
}
