package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.LabourEigenvalue;
import com.zhaocai.archives.main.domain.LaborServicesFeatureValue;

import java.util.List;
import java.util.Map;

/**
 * 劳务特征值Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILabourEigenvalueService extends IService<LabourEigenvalue> {
    /**
     * 查询劳务特征值
     *
     * @param id 劳务特征值主键
     * @return 劳务特征值
     */
    public LabourEigenvalue selectLabourEigenvalueById(Long id);

    public List<LabourEigenvalue> selectLabourEigenvalueListNoChange(LabourEigenvalue labourEigenvalue);


    /**
     * 查询劳务特征值列表
     *
     * @param labourEigenvalue 劳务特征值
     * @return 劳务特征值集合
     */
    public List<LabourEigenvalue> selectLabourEigenvalueList(LabourEigenvalue labourEigenvalue);

    /**
     * 新增劳务特征值
     *
     * @param labourEigenvalue 劳务特征值
     * @return 结果
     */
    public int insertLabourEigenvalue(LabourEigenvalue labourEigenvalue);

    /**
     * 修改劳务特征值
     *
     * @param labourEigenvalue 劳务特征值
     * @return 结果
     */
    public int updateLabourEigenvalue(LabourEigenvalue labourEigenvalue);

    /**
     * 批量删除劳务特征值
     *
     * @param ids 需要删除的劳务特征值主键集合
     * @return 结果
     */
    public boolean deleteLabourEigenvalueByIds(Long[] ids);

    /**
     * 删除劳务特征值信息
     *
     * @param id 劳务特征值主键
     * @return 结果
     */
    public int deleteLabourEigenvalueById(Long id);

    List<LabourEigenvalue> initData(LabourEigenvalue labourEigenvalue);


    void addTypeByMain(LaborServicesFeatureValue mtrClass);

    void updateByHostId(LaborServicesFeatureValue mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    int addToMain(LabourEigenvalue labourEigenvalue);

    int associationToMain(LabourEigenvalue labourEigenvalue);

    int unAssociationToMain(LabourEigenvalue labourEigenvalue);

    long selectLabourEigenvalueListCount(LabourEigenvalue labourEigenvalue);

    List<LabourEigenvalue> getProcessed(String organCode);
}
