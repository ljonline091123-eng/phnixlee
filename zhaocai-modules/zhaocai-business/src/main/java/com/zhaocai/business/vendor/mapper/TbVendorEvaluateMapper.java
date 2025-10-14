package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 供应商评价Mapper接口
 *
 * @author xw
 * @date 2025-10-11
 */
public interface TbVendorEvaluateMapper extends BaseMapper<TbVendorEvaluate> {

      IPage<TbVendorEvaluate> listPage(Page mybatisPage, @Param("queryVO") TbVendorEvaluateQueryVo queryVO);

      List<TbVendorEvaluate> getList(@Param("queryVO") TbVendorEvaluateQueryVo queryVO);
}
