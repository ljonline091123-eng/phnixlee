package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;

import java.util.List;
import java.util.Map;

/**
 * 专业分包特征项Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ISubcontractingItemService extends IService<SubcontractingItem> {
    /**
     * 查询专业分包特征项
     *
     * @param id 专业分包特征项主键
     * @return 专业分包特征项
     */
    public SubcontractingItem selectSubcontractingItemById(Long id);

    public List<SubcontractingItem> selectSubcontractingItemListNoChange(SubcontractingItem subcontractingItem);

    /**
     * 查询专业分包特征项列表
     *
     * @param subcontractingItem 专业分包特征项
     * @return 专业分包特征项集合
     */
    public List<SubcontractingItem> selectSubcontractingItemList(SubcontractingItem subcontractingItem);

    /**
     * 新增专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    public int insertSubcontractingItem(SubcontractingItem subcontractingItem);

    /**
     * 修改专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    public int updateSubcontractingItem(SubcontractingItem subcontractingItem);

    /**
     * 批量删除专业分包特征项
     *
     * @param ids 需要删除的专业分包特征项主键集合
     * @return 结果
     */
    public boolean deleteSubcontractingItemByIds(Long[] ids);

    /**
     * 删除专业分包特征项信息
     *
     * @param id 专业分包特征项主键
     * @return 结果
     */
    public int deleteSubcontractingItemById(Long id);

    List<SubcontractingItem> initData(SubcontractingItem subcontractingItem);


    void addTypeByMain(MajorSubcontractingFeature mtrClass);

    void updateByHostId(MajorSubcontractingFeature mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);


    int addToMain(SubcontractingItem subcontractingItem);

    int associationToMain(SubcontractingItem subcontractingItem);

    int unAssociationToMain(SubcontractingItem subcontractingItem);

    long selectSubcontractingItemListCount(SubcontractingItem subcontractingItem);

    List<SubcontractingItem> getProcessed(String organCode);
}
