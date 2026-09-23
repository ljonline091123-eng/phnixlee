package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.domain.ProblemReport;
import com.zhaocai.business.report.vo.EvaluationBadReportVo;
import com.zhaocai.business.report.vo.ProblemReportVo;

import java.util.List;

public interface ProblemReportMapper extends BaseMapper<ProblemReport> {

    List<ProblemReportVo> selectByDepartment(ProblemReportVo queryVO);

    List<ProblemReportVo> select(ProblemReportVo queryVO);

    List<ProblemReportVo> getProblemByDept(ProblemReportVo queryVo);

    List<EvaluationBadReportVo> selectEvaluationBad(EvaluationBadReportVo queryVO);

    List<ProblemReport> selectAll();
}
