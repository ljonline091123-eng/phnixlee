package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.zhaocai.archives.main.domain.MtrFeatureValue;

import java.util.List;
import java.util.Map;

/**
 * 材料特征值Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMaterialEigenvalueService extends IService<MaterialEigenvalue> {
    /**
     * 查询材料特征值
     *
     * @param id 材料特征值主键
     * @return 材料特征值
     */
    public MaterialEigenvalue selectMaterialEigenvalueById(Long id);

    public List<MaterialEigenvalue> selectMaterialEigenvalueListNoChange(MaterialEigenvalue materialEigenvalue);

    /**
     * 查询材料特征值列表
     *
     * @param materialEigenvalue 材料特征值
     * @return 材料特征值集合
     */
    public List<MaterialEigenvalue> selectMaterialEigenvalueList(MaterialEigenvalue materialEigenvalue);

    /**
     * 新增材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    public int insertMaterialEigenvalue(MaterialEigenvalue materialEigenvalue);

    /**
     * 修改材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    public int updateMaterialEigenvalue(MaterialEigenvalue materialEigenvalue);

    /**
     * 批量删除材料特征值
     *
     * @param ids 需要删除的材料特征值主键集合
     * @return 结果
     */
    public Boolean deleteMaterialEigenvalueByIds(Long[] ids);

    /**
     * 删除材料特征值信息
     *
     * @param id 材料特征值主键
     * @return 结果
     */
    public int deleteMaterialEigenvalueById(Long id);

    List<MaterialEigenvalue> initData(MaterialEigenvalue materialEigenvalue);


    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    void updateByHostId(MtrFeatureValue mtrFeatureValue);

    void addByMain(MtrFeatureValue mtrFeatureValue);


    int addToMain(MaterialEigenvalue materialEigenvalue);

    int associationToMain(MaterialEigenvalue materialEigenvalue);

    int unAssociationToMain(MaterialEigenvalue materialEigenvalue);

    long selectMaterialEigenvalueListCount(MaterialEigenvalue materialEigenvalue);

    List<MaterialEigenvalue> getProcessed(String organCode);
}
