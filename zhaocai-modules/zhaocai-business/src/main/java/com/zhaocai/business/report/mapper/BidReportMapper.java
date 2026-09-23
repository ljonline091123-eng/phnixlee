package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.domain.BidReport;
import com.zhaocai.business.report.vo.VBidCountVo;

import java.util.List;

public interface BidReportMapper extends BaseMapper<BidReport> {

    List<VBidCountVo> select(VBidCountVo queryVo);

    List<VBidCountVo> getBidCountByDept(VBidCountVo queryVo);

    List<BidReport> selectAll();
}
