package com.zhaocai.archives.main.service;

import java.util.List;

import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.MtrClass;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.MtrClassExcelData;

/**
 * 材料分类主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMtrClassService  extends IService<MtrClass>
{
    /**
     * 查询材料分类主
     *
     * @param id 材料分类主主键
     * @return 材料分类主
     */
    public MtrClass selectMtrClassById(String id);

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主集合
     */
    public List<MtrClass> selectMtrClassList(MtrClass mtrClass);


    public List<MtrClass> selectMtrClassListNoChange(MtrClass mtrClass);

    /**
     * 新增材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    public int insertMtrClass(MtrClass mtrClass);

    /**
     * 修改材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    public int updateMtrClass(MtrClass mtrClass);

    /**
     * 批量删除材料分类主
     *
     * @param ids 需要删除的材料分类主主键集合
     * @return 结果
     */
    public Boolean deleteMtrClassByIds(String[] ids);

    /**
     * 删除材料分类主信息
     *
     * @param id 材料分类主主键
     * @return 结果
     */
    public int deleteMtrClassById(String id);

    MtrClass initCode(MtrClass mtrClass);

    List<MaterialTypeTree> getMtrClassTree(MtrClass materialType);

    long selectMtrClassListCount(MtrClass mtrClass);

    String importData(List<MtrClassExcelData> userList, Boolean updateSupport, String operName);
}
