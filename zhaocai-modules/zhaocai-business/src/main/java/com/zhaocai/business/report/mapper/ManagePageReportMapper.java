package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.ManagePageReportVo;

import java.util.List;

public interface ManagePageReportMapper extends BaseMapper<ManagePageReportVo> {
    List<ManagePageReportVo> getManagePageReportResult(ManagePageReportVo managePage);
}
