package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.ProjectCertificationType;
import com.zhaocai.business.procurement.vo.res.ProjectCertificationTypeVO;

import java.util.List;

public interface IProjectCertificationTypeService extends IService<ProjectCertificationType> {

    List<ProjectCertificationTypeVO> getProjectCertificationTypeTree();

    /*
    * 查询工程类型对应的工程类别名
    * */
    String getProjectCertificationTypeName(String codes);

    ProjectCertificationType getProjectCertificationTypeByCodeAndType(String code, String type);
}
