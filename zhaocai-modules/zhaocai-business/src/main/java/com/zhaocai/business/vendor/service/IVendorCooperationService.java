package com.zhaocai.business.vendor.service;

import com.zhaocai.business.vendor.vo.req.VendorCooperationAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperationListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperativePartnerListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperativePartnerListVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 供应商合作记录服务
 *
 * @author chenming
 * @date 2024-06-25
 */
public interface IVendorCooperationService {

    /**
     * 获取供应商合作记录
     * @param queryVO
     * @return
     */
    PageResult<VendorCooperationListVO> listPage(VendorCooperationListQueryVO queryVO);

    /**
     * 获取供应商合作单位
     * @param queryVO
     * @return
     */
    PageResult<VendorCooperativePartnerListVO> listVendorCooperativePartner(VendorCooperativePartnerListQueryVO queryVO);

    /**
     * 供应商合作记录详情
     * @param queryVO
     * @return
     */
    List<VendorCooperationAgreementVO> listDetail(VendorCooperationAgreementListQueryVO queryVO);
}
