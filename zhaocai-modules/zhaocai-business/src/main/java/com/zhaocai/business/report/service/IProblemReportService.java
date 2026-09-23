package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.EvaluationBadReportVo;
import com.zhaocai.business.report.vo.ProblemReportVo;

import java.util.List;
import java.util.Map;

/**
 * 问题报表Service接口
 */
public interface IProblemReportService {

    Map<String, Object> getProblemReport(ProblemReportVo queryVO);

    List<ProblemReportVo> getInitialInfo(ProblemReportVo queryVO);

    List<ProblemReportVo> getProblemNext(ProblemReportVo queryVO);

    Boolean handleProblemReport();

    Map<String, Object> problemReportExport(ProblemReportVo vo);

    /**
     * 供应商评价不合格记录(tab2)
     */
    Map<String, Object> getEvaluationBadReport(EvaluationBadReportVo queryVO);

    /**
     * 供应商评价不合格记录导出(tab2)
     */
    Map<String, Object> evaluationBadReportExport(EvaluationBadReportVo queryVO);
}
