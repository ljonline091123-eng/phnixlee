package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.LabourEigenvalue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;

/**
 * 劳务特征值Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface LabourEigenvalueMapper extends BaseMapper<LabourEigenvalue>
{
    /**
     * 查询劳务特征值
     *
     * @param id 劳务特征值主键
     * @return 劳务特征值
     */
    public LabourEigenvalue selectLabourEigenvalueById(Long id);

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
     * 删除劳务特征值
     *
     * @param id 劳务特征值主键
     * @return 结果
     */
    public int deleteLabourEigenvalueById(Long id);

    /**
     * 批量删除劳务特征值
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabourEigenvalueByIds(Long[] ids);

    public int updateDelByItemId(Long[] ids);

    long selectLabourEigenvalueListCount(LabourEigenvalue labourEigenvalue);


    List<LabourEigenvalue> getProcessed(String organCode);
}
