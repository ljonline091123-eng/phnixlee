package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.main.domain.MtrArchives;

import java.util.List;
import java.util.Map;

/**
 * 材料详情Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMaterialDetailsService extends IService<MaterialDetails> {

    public List<MaterialDetails> selectMaterialDetailsListNoChange(MaterialDetails materialDetails);

    /**
     * 查询材料详情
     *
     * @param id 材料详情主键
     * @return 材料详情
     */
    public MaterialDetails selectMaterialDetailsById(Long id);

    /**
     * 查询材料详情列表
     *
     * @param materialDetails 材料详情
     * @return 材料详情集合
     */
    public List<MaterialDetails> selectMaterialDetailsList(MaterialDetails materialDetails);

    /**
     * 新增材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    public int insertMaterialDetails(MaterialDetails materialDetails);

    /**
     * 修改材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    public int updateMaterialDetails(MaterialDetails materialDetails);

    /**
     * 批量删除材料详情
     *
     * @param ids 需要删除的材料详情主键集合
     * @return 结果
     */
    public Boolean deleteMaterialDetailsByIds(Long[] ids);

    /**
     * 删除材料详情信息
     *
     * @param id 材料详情主键
     * @return 结果
     */
    public int deleteMaterialDetailsById(Long id);


    List<MaterialDetails> initData(MaterialDetails materialDetails);


    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    void updateByHostId(MtrArchives mtrArchives);

    void addByMain(MtrArchives mtrArchives);


    int addToMain(MaterialDetails materialDetails);

    int associationToMain(MaterialDetails materialDetails);

    int unAssociationToMain(MaterialDetails materialDetails);

    long selectMaterialDetailsListCount(MaterialDetails materialDetails);

    List<MaterialDetails> getProcessed(String organCode);
}
