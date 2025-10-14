package com.zhaocai.business.vendor.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.mapper.TbVendorEvaluateMapper;
import com.zhaocai.business.vendor.service.ITbVendorEvaluateService;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import com.zhaocai.common.core.bean.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商评价Service业务层处理
 *
 * @author xw
 * @date 2025-10-11
 */
@Service
public class TbVendorEvaluateServiceImpl extends ServiceImpl<TbVendorEvaluateMapper, TbVendorEvaluate> implements ITbVendorEvaluateService {

    @Override
    public PageResult<TbVendorEvaluate> listPage(TbVendorEvaluateQueryVo queryVO) {
        IPage<TbVendorEvaluate> iPage = baseMapper.listPage(queryVO.toMybatisPage(), queryVO);

        return new PageResult<>(iPage);
    }

}
