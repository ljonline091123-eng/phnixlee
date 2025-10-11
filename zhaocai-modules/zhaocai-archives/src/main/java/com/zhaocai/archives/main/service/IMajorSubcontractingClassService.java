package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.domain.MajorSubcontractingClassExcelData;

import java.util.List;

/**
 * 专业分包分类主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IMajorSubcontractingClassService extends IService<MajorSubcontractingClass> {
    /**
     * 查询专业分包分类主
     *
     * @param id 专业分包分类主主键
     * @return 专业分包分类主
     */
    public MajorSubcontractingClass selectMajorSubcontractingClassById(String id);

    /**
     * 查询专业分包分类主列表
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 专业分包分类主集合
     */
    public List<MajorSubcontractingClass> selectMajorSubcontractingClassList(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 新增专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    public int insertMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 修改专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    public int updateMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass);

    /**
     * 批量删除专业分包分类主
     *
     * @param ids 需要删除的专业分包分类主主键集合
     * @return 结果
     */
    public boolean deleteMajorSubcontractingClassByIds(String[] ids);

    /**
     * 删除专业分包分类主信息
     *
     * @param id 专业分包分类主主键
     * @return 结果
     */
    public int deleteMajorSubcontractingClassById(String id);

    List<SubcontractingTypeTree> getMajorSubcontractingClassTree();

    MajorSubcontractingClass initCode(MajorSubcontractingClass aClass);

    long selectMajorSubcontractingClassListCount(MajorSubcontractingClass majorSubcontractingClass);

    String importData(List<MajorSubcontractingClassExcelData> userList, boolean updateSupport, String operName);

    List<SubcontractingTypeTree> getMajorSubcontractingClassTreeTwo();
}
