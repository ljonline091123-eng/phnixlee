package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.LabourItem;
import com.zhaocai.archives.main.domain.LaborServicesFeature;

import java.util.List;
import java.util.Map;

/**
 * 劳务特征项Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILabourItemService extends IService<LabourItem> {
    /**
     * 查询劳务特征项
     *
     * @param id 劳务特征项主键
     * @return 劳务特征项
     */
    public LabourItem selectLabourItemById(Long id);

    public List<LabourItem> selectLabourItemListNoChange(LabourItem labourItem);


    /**
     * 查询劳务特征项列表
     *
     * @param labourItem 劳务特征项
     * @return 劳务特征项集合
     */
    public List<LabourItem> selectLabourItemList(LabourItem labourItem);

    /**
     * 新增劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    public int insertLabourItem(LabourItem labourItem);

    /**
     * 修改劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    public int updateLabourItem(LabourItem labourItem);

    /**
     * 批量删除劳务特征项
     *
     * @param ids 需要删除的劳务特征项主键集合
     * @return 结果
     */
    public boolean deleteLabourItemByIds(Long[] ids);

    /**
     * 删除劳务特征项信息
     *
     * @param id 劳务特征项主键
     * @return 结果
     */
    public int deleteLabourItemById(Long id);

    List<LabourItem> initData(LabourItem labourItem);

    void addTypeByMain(LaborServicesFeature mtrClass);

    void updateByHostId(LaborServicesFeature mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);


    int addToMain(LabourItem labourItem);

    int associationToMain(LabourItem labourItem);

    int unAssociationToMain(LabourItem labourItem);

    long selectLabourItemListCount(LabourItem labourItem);

    List<LabourItem> getProcessed(String organCode);
}
