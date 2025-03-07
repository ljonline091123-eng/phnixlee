package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.LabourDetails;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 劳务详情Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LabourDetailsMapper extends BaseMapper<LabourDetails> {
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

    public long selectLabourDetailsListCount(LabourDetails labourDetails);

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
     * 删除劳务详情
     *
     * @param id 劳务详情主键
     * @return 结果
     */
    public int deleteLabourDetailsById(Long id);

    /**
     * 批量删除劳务详情
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabourDetailsByIds(Long[] ids);

    Integer getMaxCode(@Param("typeId") Long typeId, @Param("organCode") String organCode, @Param("oldCode") String labourCode);


    List<LabourDetails> getProcessed(String organCode);
}
