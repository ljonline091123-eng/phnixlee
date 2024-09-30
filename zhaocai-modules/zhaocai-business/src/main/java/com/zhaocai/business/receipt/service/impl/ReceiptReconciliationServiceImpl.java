package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhaocai.business.receipt.domain.ReceiptReconciliationDetail;
import com.zhaocai.business.receipt.mapper.ReceiptReconciliationMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptReconciliationDetailVO;
import com.zhaocai.business.receipt.vo.req.ReceiptReconciliationVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailListVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailPageVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationListVO;
import com.zhaocai.business.receipt.vo.res.query.ReconciliationQueryVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.domain.ReceiptReconciliation;
import com.zhaocai.business.receipt.service.IReceiptReconciliationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 材料对账单Service业务层处理
 *
 * @author zhangxu
 * @date 2024-09-06
 */
@Service
public class ReceiptReconciliationServiceImpl extends ServiceImpl<ReceiptReconciliationMapper, ReceiptReconciliation> implements IReceiptReconciliationService {

    @Autowired
    private ReceiptReconciliationDetailServiceImpl reconciliationDetailService;
    @Autowired
    private IVendorService vendorService;

    @Override
    public ReceiptReconciliationDetailPageVO getDetail(Long id) {
        //获取主表中详情页相关字段
        ReceiptReconciliationDetailPageVO detail = baseMapper.getDetail(id);
        //根据主表id查看查询从表的详情列表
        ArrayList<ReceiptReconciliationDetailListVO> detailList = new ArrayList<>();
        detailList = reconciliationDetailService.getDetailList(id);
        //将详情列表设置到详情页
        detail.setReconciliationDetailList(detailList);
        return detail;
    }

    @Override
    public PageResult<ReceiptReconciliationListVO> page(ReconciliationQueryVO queryVO) {
        queryVO.setVendorId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptReconciliationListVO> iPage = baseMapper.page(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(iPage);
    }

    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReconciliation(ReceiptReconciliationVO reconciliationVO) {
        //获取第三方数据
        String thirdId = reconciliationVO.getId();
        String thirdCreateBy = reconciliationVO.getCreateBy();
        String thirdCreateTime = reconciliationVO.getCreateTime();

        //设置材料对账单参数
        ReceiptReconciliation reconciliation = BeanCopierUtil.copyBean(reconciliationVO, ReceiptReconciliation.class);
        reconciliation.setId(null);
        reconciliation.setCreateBy(null);
        reconciliation.setCreateTime(null);
        reconciliation.setThirdId(thirdId);
        reconciliation.setThirdCreateBy(thirdCreateBy);
        reconciliation.setThirdCreateTime(thirdCreateTime);

        //新增材料对账单
        boolean isSaved = this.save(reconciliation);

        // 材料对账单id
        Long id = reconciliation.getId();
        //如果对账单添加成功,保存对象单详情列表
        if (isSaved) {
            return saveReconciliationDetail(reconciliationVO.getDataRespList(), id);
        }
        return false;
    }

    public boolean saveReconciliationDetail(List<ReceiptReconciliationDetailVO> reconciliationDetailVOList, long id) {
        //如果对账单详情列表不为空,获取并新增到数据库
        if (reconciliationDetailVOList != null) {
            //new一个对账单详情列表
            ArrayList<ReceiptReconciliationDetail> detailList = new ArrayList<>();
            for (ReceiptReconciliationDetailVO reconciliationDetailVO : reconciliationDetailVOList) {
                ReceiptReconciliationDetail reconciliationDetail =
                        BeanCopierUtil.copyBean(reconciliationDetailVO, ReceiptReconciliationDetail.class);
                //设置对账单详情的对账单id
                reconciliationDetail.setReconciliationId(id);
                //将对账单详情添加到list集合
                detailList.add(reconciliationDetail);
            }
            //新增对账单详情列表
            return reconciliationDetailService.saveBatch(detailList);
        }
        return false;
    }
}
