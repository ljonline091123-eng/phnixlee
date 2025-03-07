package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 材料特征值Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MaterialEigenvalueMapper extends BaseMapper<MaterialEigenvalue>
{
    /**
     * 查询材料特征值
     *
     * @param id 材料特征值主键
     * @return 材料特征值
     */
    public MaterialEigenvalue selectMaterialEigenvalueById(Long id);

    /**
     * 查询材料特征值列表
     *
     * @param materialEigenvalue 材料特征值
     * @return 材料特征值集合
     */
    public List<MaterialEigenvalue> selectMaterialEigenvalueList(MaterialEigenvalue materialEigenvalue);

    /**
     * 新增材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    public int insertMaterialEigenvalue(MaterialEigenvalue materialEigenvalue);

    /**
     * 修改材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    public int updateMaterialEigenvalue(MaterialEigenvalue materialEigenvalue);

    /**
     * 删除材料特征值
     *
     * @param id 材料特征值主键
     * @return 结果
     */
    public int deleteMaterialEigenvalueById(Long id);

    /**
     * 批量删除材料特征值
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMaterialEigenvalueByIds(Long[] ids);

    public int updateDelByItemId(Long[] ids);

    long selectMaterialEigenvalueListCount(MaterialEigenvalue materialEigenvalue);

    List<MaterialEigenvalue> getProcessed(String organCode);
}
