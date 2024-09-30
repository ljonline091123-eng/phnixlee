package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.VBidCountVo;

import java.util.List;

public interface VBidCountMapper extends BaseMapper<VBidCountVo> {

    List<VBidCountVo> select(VBidCountVo vBidCountVo);
}
