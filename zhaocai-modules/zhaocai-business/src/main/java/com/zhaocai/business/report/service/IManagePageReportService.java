package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ManagePageReportVo;
import com.zhaocai.system.api.domain.SysDept;

import java.util.List;
import java.util.Map;

/**
 * 公司/集团管理组织Service接口
 */
public interface IManagePageReportService {
    Map<String, Object> managePageReport(ManagePageReportVo managePage);

    List<SysDept> managePageReportDept(ManagePageReportVo managePage);

    List<SysDept> getOrgList(String orgId);
}
