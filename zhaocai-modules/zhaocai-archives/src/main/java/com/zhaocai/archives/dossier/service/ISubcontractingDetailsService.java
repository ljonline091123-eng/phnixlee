package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;

import java.util.List;
import java.util.Map;

/**
 * 专业分包详情Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ISubcontractingDetailsService extends IService<SubcontractingDetails> {

    public List<SubcontractingDetails> selectSubcontractingDetailsListNoChange(SubcontractingDetails subcontractingDetails);

    /**
     * 查询专业分包详情
     *
     * @param id 专业分包详情主键
     * @return 专业分包详情
     */
    public SubcontractingDetails selectSubcontractingDetailsById(Long id);

    /**
     * 查询专业分包详情列表
     *
     * @param subcontractingDetails 专业分包详情
     * @return 专业分包详情集合
     */
    public List<SubcontractingDetails> selectSubcontractingDetailsList(SubcontractingDetails subcontractingDetails);

    /**
     * 新增专业分包详情
     *
     * @param subcontractingDetails 专业分包详情
     * @return 结果
     */
    public int insertSubcontractingDetails(SubcontractingDetails subcontractingDetails);

    /**
     * 修改专业分包详情
     *
     * @param subcontractingDetails 专业分包详情
     * @return 结果
     */
    public int updateSubcontractingDetails(SubcontractingDetails subcontractingDetails);

    /**
     * 批量删除专业分包详情
     *
     * @param ids 需要删除的专业分包详情主键集合
     * @return 结果
     */
    public boolean deleteSubcontractingDetailsByIds(Long[] ids);

    /**
     * 删除专业分包详情信息
     *
     * @param id 专业分包详情主键
     * @return 结果
     */
    public int deleteSubcontractingDetailsById(Long id);

    List<SubcontractingDetails> initData(SubcontractingDetails subcontractingDetails);


    void addTypeByMain(MajorSubcontractingArchives mtrClass);

    void updateByHostId(MajorSubcontractingArchives mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);

    int addToMain(SubcontractingDetails subcontractingDetails);

    int associationToMain(SubcontractingDetails subcontractingDetails);

    int unAssociationToMain(SubcontractingDetails subcontractingDetails);

    SubcontractingDetails initDetails(SubcontractingDetails subcontractingDetails);

    long selectSubcontractingDetailsListCount(SubcontractingDetails subcontractingDetails);

    List<SubcontractingDetails> getProcessed(String organCode);
}
