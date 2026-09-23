package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.business.pub.domain.Message;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 供应商评价Service接口
 *
 * @author xw
 * @date 2025-10-11
 */
public interface ITbVendorEvaluateService extends IService<TbVendorEvaluate> {

    PageResult<TbVendorEvaluate> listPage(TbVendorEvaluateQueryVo tbVendorEvaluate);

    List<TbVendorEvaluate> getList(TbVendorEvaluateQueryVo queryVO);

    Long getIsAppeal(Long id);

    boolean sendMessage(TbVendorEvaluate message);
}
