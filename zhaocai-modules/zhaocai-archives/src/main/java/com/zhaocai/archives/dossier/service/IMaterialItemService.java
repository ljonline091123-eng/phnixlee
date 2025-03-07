package com.zhaocai.archives.dossier.service;

import java.util.List;
import java.util.Map;

import com.zhaocai.archives.dossier.domain.MaterialItem;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MtrFeature;

/**
 * 材料特征项Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMaterialItemService  extends IService<MaterialItem>
{
    /**
     * 查询材料特征项
     *
     * @param id 材料特征项主键
     * @return 材料特征项
     */
    public MaterialItem selectMaterialItemById(Long id);

    public List<MaterialItem> selectMaterialItemListNoChange(MaterialItem materialItem);

    /**
     * 查询材料特征项列表
     *
     * @param materialItem 材料特征项
     * @return 材料特征项集合
     */
    public List<MaterialItem> selectMaterialItemList(MaterialItem materialItem);


    /**
     * 新增材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    public int insertMaterialItem(MaterialItem materialItem);

    /**
     * 修改材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    public int updateMaterialItem(MaterialItem materialItem);

    /**
     * 批量删除材料特征项
     *
     * @param ids 需要删除的材料特征项主键集合
     * @return 结果
     */
    public Boolean deleteMaterialItemByIds(Long[] ids);

    /**
     * 删除材料特征项信息
     *
     * @param id 材料特征项主键
     * @return 结果
     */
    public int deleteMaterialItemById(Long id);

    List<MaterialItem> initData(MaterialItem materialItem);

    void updateByHostId(MtrFeature mtrFeature);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    void addByMain(MtrFeature mtrFeature);

    int addToMain(MaterialItem materialItem);

    int associationToMain(MaterialItem materialItem);

    int unAssociationToMain(MaterialItem materialItem);

    long selectMaterialItemListCount(MaterialItem materialItem);

    List<MaterialItem> getProcessed(String organCode);
}
