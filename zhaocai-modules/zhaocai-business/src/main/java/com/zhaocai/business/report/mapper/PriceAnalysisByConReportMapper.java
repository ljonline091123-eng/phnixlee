package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.PriceAnalysisByConReportVo;

import java.util.List;

public interface PriceAnalysisByConReportMapper extends BaseMapper<PriceAnalysisByConReportVo> {

    /**
     * 获取劳务分包价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisLabor(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 获取设备租赁（机械）价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisLeasedDevice(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 获取租赁材料价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisLeasedMaterials(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 获取购买材料价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisMaterials(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 获取其他价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisOther(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 获取专业分包价格分析
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> getVPriceAnalysisSpecialty(PriceAnalysisByConReportVo priceAnalysis);
}
