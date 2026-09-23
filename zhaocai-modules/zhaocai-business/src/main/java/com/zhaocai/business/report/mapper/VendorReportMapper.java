package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.domain.VendorReport;
import com.zhaocai.business.report.vo.req.VendorReportQueryVo;
import com.zhaocai.business.report.vo.res.VendorReportListVo;

import java.util.List;

public interface VendorReportMapper extends BaseMapper<VendorReport> {

    List<VendorReportListVo> select(VendorReportQueryVo queryVO);

    List<VendorReport> selectAll();
}
