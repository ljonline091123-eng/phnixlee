package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingEigenvalue;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;

import java.util.List;
import java.util.Map;

/**
 * 专业分包特征值Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ISubcontractingEigenvalueService extends IService<SubcontractingEigenvalue> {
    /**
     * 查询专业分包特征值
     *
     * @param id 专业分包特征值主键
     * @return 专业分包特征值
     */
    public SubcontractingEigenvalue selectSubcontractingEigenvalueById(Long id);

    public List<SubcontractingEigenvalue> selectSubcontractingEigenvalueListNoChange(SubcontractingEigenvalue subcontractingEigenvalue);


    /**
     * 查询专业分包特征值列表
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 专业分包特征值集合
     */
    public List<SubcontractingEigenvalue> selectSubcontractingEigenvalueList(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 新增专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    public int insertSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 修改专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    public int updateSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 批量删除专业分包特征值
     *
     * @param ids 需要删除的专业分包特征值主键集合
     * @return 结果
     */
    public boolean deleteSubcontractingEigenvalueByIds(Long[] ids);

    /**
     * 删除专业分包特征值信息
     *
     * @param id 专业分包特征值主键
     * @return 结果
     */
    public int deleteSubcontractingEigenvalueById(Long id);

    List<SubcontractingEigenvalue> initData(SubcontractingEigenvalue subcontractingEigenvalue);

    void addTypeByMain(MajorSubcontractingFeatureValue mtrClass);

    void updateByHostId(MajorSubcontractingFeatureValue mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    int addToMain(SubcontractingEigenvalue subcontractingEigenvalue);

    int associationToMain(SubcontractingEigenvalue subcontractingEigenvalue);

    int unAssociationToMain(SubcontractingEigenvalue subcontractingEigenvalue);

    long selectSubcontractingEigenvalueListCount(SubcontractingEigenvalue subcontractingEigenvalue);

    List<SubcontractingEigenvalue> getProcessed(String organCode);
}
