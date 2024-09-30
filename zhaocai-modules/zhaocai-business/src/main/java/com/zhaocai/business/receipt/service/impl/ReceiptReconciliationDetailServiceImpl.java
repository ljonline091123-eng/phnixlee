package com.zhaocai.business.receipt.service.impl;

import com.zhaocai.business.receipt.mapper.ReceiptReconciliationDetailMapper;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailListVO;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.domain.ReceiptReconciliationDetail;
import com.zhaocai.business.receipt.service.IReceiptReconciliationDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;

/**
 * 材料对账单详情Service业务层处理
 *
 * @author zhagnxu
 * @date 2024-09-07
 */
@Service
public class ReceiptReconciliationDetailServiceImpl extends ServiceImpl<ReceiptReconciliationDetailMapper, ReceiptReconciliationDetail> implements IReceiptReconciliationDetailService {
    @Override
    public ArrayList<ReceiptReconciliationDetailListVO> getDetailList(Long id) {
        return baseMapper.getDetailList(id);
    }
}
