package com.zhaocai.archives.dossier.service;

import java.util.List;
import java.util.Map;

import com.zhaocai.archives.dossier.domain.LabourDetails;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.main.domain.DeviceArchives;
import com.zhaocai.archives.main.domain.LaborServicesArchives;

/**
 * 劳务详情Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface ILabourDetailsService  extends IService<LabourDetails>
{

    public List<LabourDetails> selectLabourDetailsListNoChange(LabourDetails labourDetails);

    /**
     * 查询劳务详情
     *
     * @param id 劳务详情主键
     * @return 劳务详情
     */
    public LabourDetails selectLabourDetailsById(Long id);

    /**
     * 查询劳务详情列表
     *
     * @param labourDetails 劳务详情
     * @return 劳务详情集合
     */
    public List<LabourDetails> selectLabourDetailsList(LabourDetails labourDetails);

    /**
     * 新增劳务详情
     *
     * @param labourDetails 劳务详情
     * @return 结果
     */
    public int insertLabourDetails(LabourDetails labourDetails);

    /**
     * 修改劳务详情
     *
     * @param labourDetails 劳务详情
     * @return 结果
     */
    public int updateLabourDetails(LabourDetails labourDetails);

    /**
     * 批量删除劳务详情
     *
     * @param ids 需要删除的劳务详情主键集合
     * @return 结果
     */
    public boolean deleteLabourDetailsByIds(Long[] ids);

    /**
     * 删除劳务详情信息
     *
     * @param id 劳务详情主键
     * @return 结果
     */
    public int deleteLabourDetailsById(Long id);

    List<LabourDetails> initData(LabourDetails labourDetails);


    void addTypeByMain(LaborServicesArchives mtrClass);

    void updateByHostId(LaborServicesArchives mtrClass);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    int addToMain(LabourDetails labourDetails);

    int associationToMain(LabourDetails labourDetails);

    int unAssociationToMain(LabourDetails labourDetails);

    public LabourDetails initDetails(LabourDetails materialDetails);

    long selectLabourDetailsListCount(LabourDetails labourDetails);

    List<LabourDetails> getProcessed(String organCode);
}
