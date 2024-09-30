package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.agreement.vo.req.AgreementSchemeQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementSchemeListVO;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.res.BiddingSchemeListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectDataVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购方案Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ProcurementSchemeMapper extends BaseMapper<ProcurementScheme> {

    /**
     * 获取采购方案列表
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ProcurementSchemeListVO> selectPageList(Page mybatisPage, @Param("queryVO") ProcurementSchemeListQueryVO queryVO);

    IPage<BiddingSchemeListVO> selectBiddingSchemePageList(Page mybatisPage, @Param("queryVO") BiddingSchemeListQueryVO queryVO);

    /**
     * 获取采购方案绑定的采购计划
     * @param schemeId
     * @return
     */
    List<ProcurementPlanListVO> selectProcurementPlanListByScheme(@Param("schemeId") Long schemeId);

    /**
     * 获取可签订合同的采购方案
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<AgreementSchemeListVO> selectSignAgreementSchemeList(Page mybatisPage, @Param("queryVO") AgreementSchemeQueryVO queryVO);

    /**
     * 通过采购方案id获取最小核算项目信息
     * @param schemeId
     * @return
     */
    List<MinProjectDataVO> selectDataByScheme(@Param("schemeId") Long schemeId);
}
