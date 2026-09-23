package com.zhaocai.business.pub.mapper;

import java.util.List;
import com.zhaocai.business.pub.domain.ProjectWork;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 项目明细Mapper接口
 *
 * @author cff
 * @date 2024-09-26
 */
public interface ProjectWorkMapper extends BaseMapper<ProjectWork> {
    /**
     * 查询项目明细
     *
     * @param id 项目明细主键
     * @return 项目明细
     */
    public ProjectWork selectProjectWorkById(Long id);

    /**
     * 查询项目明细列表
     *
     * @param projectWork 项目明细
     * @return 项目明细集合
     */
    public List<ProjectWork> selectProjectWorkList(ProjectWork projectWork);

    /**
     * 新增项目明细
     *
     * @param projectWork 项目明细
     * @return 结果
     */
    public int insertProjectWork(ProjectWork projectWork);

    /**
     * 修改项目明细
     *
     * @param projectWork 项目明细
     * @return 结果
     */
    public int updateProjectWork(ProjectWork projectWork);

    /**
     * 删除项目明细
     *
     * @param thirdId 项目明细主键
     * @return 结果
     */
    public int deleteProjectWorkByThirdId(String thirdId);

    /**
     * 批量删除项目明细
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectWorkByIds(Long[] ids);
}
