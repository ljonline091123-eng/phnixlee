package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingEigenvalue;

import java.util.List;

/**
 * 专业分包特征值Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface SubcontractingEigenvalueMapper extends BaseMapper<SubcontractingEigenvalue> {
    /**
     * 查询专业分包特征值
     *
     * @param id 专业分包特征值主键
     * @return 专业分包特征值
     */
    public SubcontractingEigenvalue selectSubcontractingEigenvalueById(Long id);

    /**
     * 查询专业分包特征值列表
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 专业分包特征值集合
     */
    public List<SubcontractingEigenvalue> selectSubcontractingEigenvalueList(SubcontractingEigenvalue subcontractingEigenvalue);


    public long selectSubcontractingEigenvalueListCount(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 新增专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    public int insertSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 修改专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    public int updateSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue);

    /**
     * 删除专业分包特征值
     *
     * @param id 专业分包特征值主键
     * @return 结果
     */
    public int deleteSubcontractingEigenvalueById(Long id);

    /**
     * 批量删除专业分包特征值
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSubcontractingEigenvalueByIds(Long[] ids);

    public int updateDelByItemId(Long[] ids);


    List<SubcontractingEigenvalue> getProcessed(String organCode);
}
