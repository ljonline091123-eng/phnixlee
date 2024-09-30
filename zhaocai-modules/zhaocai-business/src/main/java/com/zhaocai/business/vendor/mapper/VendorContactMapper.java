package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.vo.req.VendorContactListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorContactListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 供应商联系人Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface VendorContactMapper extends BaseMapper<VendorContact> {

    /**
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorContactListVO> selectVendorContactListPage(Page mybatisPage, @Param("queryVO") VendorContactListQueryVO queryVO);

}
