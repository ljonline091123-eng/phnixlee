package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.domain.ProjectCertificationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 最小核算项目信息Mapper接口
 *
 * @author WH
 * @date 2024-07-16
 */
@Mapper
public interface ProjectCertificationTypeMapper extends BaseMapper<ProjectCertificationType> {
}
