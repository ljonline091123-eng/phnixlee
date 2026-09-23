package com.zhaocai.business.pub.vo.req;

import com.zhaocai.business.pub.domain.Project;
import com.zhaocai.business.pub.domain.ProjectWork;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 项目VO
 *
 * @author cff
 * @date 2024-09-26
 */
@Data
@ApiModel(value = "ProjectVO", description = "接收项目VO")
public class ProjectVO extends Project {

    @ApiModelProperty(value =  "项目明细")
    private List<ProjectWork> projectWorksList;

}
