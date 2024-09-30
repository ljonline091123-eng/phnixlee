package com.zhaocai.business.pub.mapper;

import java.util.List;
import com.zhaocai.business.pub.domain.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.vo.req.ProjectVO;

/**
 * 项目Mapper接口
 *
 * @author cff
 * @date 2024-09-26
 */
public interface ProjectMapper extends BaseMapper<Project> {
    /**
     * 查询项目
     *
     * @param thirdId 项目主键
     * @return 项目
     */
    public Project selectProjectById(String thirdId);

    /**
     * 查询项目列表
     *
     * @param project 项目
     * @return 项目集合
     */
    public List<ProjectVO> selectProjectList(Project project);

    /**
     * 新增项目
     *
     * @param project 项目
     * @return 结果
     */
    public int insertProject(Project project);

    /**
     * 修改项目
     *
     * @param project 项目
     * @return 结果
     */
    public int updateProject(Project project);

    /**
     * 删除项目
     *
     * @param id 项目主键
     * @return 结果
     */
    public int deleteProjectById(Long id);

    /**
     * 批量删除项目
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectByIds(Long[] ids);
}
