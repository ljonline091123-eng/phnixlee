package com.zhaocai.archives.dossier.mapper;

import java.util.List;

import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 专业分包详情Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface SubcontractingDetailsMapper extends BaseMapper<SubcontractingDetails>
{
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

    public long selectSubcontractingDetailsListCount(SubcontractingDetails subcontractingDetails);

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
     * 删除专业分包详情
     *
     * @param id 专业分包详情主键
     * @return 结果
     */
    public int deleteSubcontractingDetailsById(Long id);

    /**
     * 批量删除专业分包详情
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSubcontractingDetailsByIds(Long[] ids);


    Integer getMaxCode(@Param("typeId") Long typeId, @Param("organCode") String organCode,@Param("oldCode") String subcontractingCode);


    List<SubcontractingDetails> getProcessed(String organCode);
}
