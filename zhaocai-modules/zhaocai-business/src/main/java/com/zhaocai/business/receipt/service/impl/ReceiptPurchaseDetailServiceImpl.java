package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseDetailVO;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptPurchaseDetailMapper;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import com.zhaocai.business.receipt.service.IReceiptPurchaseDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.CollectionUtils;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author WH
 * @date 2024-09-06
 */
@Service
public class ReceiptPurchaseDetailServiceImpl extends ServiceImpl<ReceiptPurchaseDetailMapper, ReceiptPurchaseDetail> implements IReceiptPurchaseDetailService {

    @Override
    public List<ReceiptPurchaseDetailVO> selectList(ReceiptPurchaseDetailVO pushPurchase) {
        List<ReceiptPurchaseDetailVO> list=baseMapper.selectList(pushPurchase);
        return list;
    }

    @Override
    public int purchaseDetailSum(String parntId) {
        ReceiptPurchaseDetailVO pushPurchase=new ReceiptPurchaseDetailVO();
        pushPurchase.setParntId(parntId);
        List<ReceiptPurchaseDetailVO> list=baseMapper.selectList(pushPurchase);
        if(!CollectionUtils.isEmpty(list)){
            Long count = list.stream().mapToLong(ReceiptPurchaseDetailVO::getResidueOrderNumber).summaryStatistics().getSum();
            return Integer.parseInt(count.toString());
        }
        return 0;
    }

    @Override
    public boolean insertPushPurchaseSaveBatch(List<ReceiptPurchaseDetail> purchaseDetailList) {
        return this.saveBatch(purchaseDetailList);
    }

    @Override
    public boolean updatePushPurchaseBatch(List<ReceiptPurchaseDetail> purchaseDetailList) {
        return this.updateBatchById(purchaseDetailList);
    }
}
