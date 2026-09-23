package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.vendor.vo.req.VendorCooperationListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperativePartnerListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperativePartnerListVO;
import org.apache.ibatis.annotations.Param;

public interface VendorCooperationMapper {

    /**
     * 获取供应商合作记录服务
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorCooperationListVO> selectVendorCooperationList(Page mybatisPage, @Param("queryVO") VendorCooperationListQueryVO queryVO);

    /**
     * 获取供应商合作单位
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorCooperativePartnerListVO> selectVendorCooperativePartner(Page mybatisPage, @Param("queryVO") VendorCooperativePartnerListQueryVO queryVO);
}
