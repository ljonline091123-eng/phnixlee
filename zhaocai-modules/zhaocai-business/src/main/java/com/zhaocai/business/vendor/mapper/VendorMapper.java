package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryDataVO;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementListDataVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementListVO;
import com.zhaocai.business.vendor.vo.res.VendorVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 供应商Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface VendorMapper extends BaseMapper<Vendor> {

    /**
     * 供应商列表查询
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorManagementListVO> selectVendorListPage(Page mybatisPage, @Param("queryVO") VendorManagementListQueryVO queryVO);

    List<VendorManagementListDataVO> selectVendorManagementList(@Param("queryVO") VendorManagementListQueryDataVO queryVO);

    /**
     * 获取供应商列表
     *
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<VendorVO> selectVendorJkpthtListPage(Page mybatisPage, @Param("queryVO") VendorManagementListQueryVO queryVO);

    int updateByMyId(@Param("id") Long id,@Param("uuid") Long uuid);
}
