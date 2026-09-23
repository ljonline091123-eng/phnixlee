package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.vo.req.AgreementJkpthtListQueryVO;
import com.zhaocai.business.agreement.vo.req.AgreementListQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementJkpthtListVO;
import com.zhaocai.business.agreement.vo.res.AgreementListVO;
import com.zhaocai.business.vendor.vo.req.VendorAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 合同基本信息Mapper接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface AgreementMapper extends BaseMapper<Agreement> {

    /**
     * 获取分页列表
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<AgreementListVO> selectPageList(Page mybatisPage, @Param("queryVO") AgreementListQueryVO queryVO);

    /**
     * 获取供应商的合同列表
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorAgreementListVO> selectVendorAgreementList(Page mybatisPage, @Param("queryVO") VendorAgreementListQueryVO queryVO);

    /**
     * 获取合同列表
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<AgreementJkpthtListVO> selectAgreementJkpthtList(Page mybatisPage, @Param("queryVO") AgreementJkpthtListQueryVO queryVO);
}
