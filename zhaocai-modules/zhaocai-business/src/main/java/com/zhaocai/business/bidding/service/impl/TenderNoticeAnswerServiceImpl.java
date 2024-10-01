package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.TenderNoticeAnswer;
import com.zhaocai.business.bidding.mapper.TenderNoticeAnswerMapper;
import com.zhaocai.business.bidding.service.ITenderNoticeAnswerService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeAnswerVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeAnswerQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeAnswerListVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 招标公告/招标内容 答疑Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderNoticeAnswerServiceImpl extends ServiceImpl<TenderNoticeAnswerMapper,TenderNoticeAnswer> implements ITenderNoticeAnswerService {

    @Autowired
    private IVendorService vendorService;

    /**
     * 查询招标公告答疑列表
     *
     * @param queryVO 招标公告答疑
     * @return
     */
    @Override
    public List<TenderNoticeAnswerListVO> getList(TenderNoticeAnswerQueryVO queryVO) {
        LambdaQueryWrapper<TenderNoticeAnswer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenderNoticeAnswer::getBusId, queryVO.getBusId());
        if (queryVO.getQuestionUser() != null){
            queryWrapper.eq(TenderNoticeAnswer::getQuestionUser, queryVO.getQuestionUser());
        }
        if (queryVO.getAnswerType() != null){
            if (queryVO.getAnswerType() == 0){
                queryWrapper.isNull(TenderNoticeAnswer::getContent);
            } else if (queryVO.getAnswerType() == 1) {
                queryWrapper.isNotNull(TenderNoticeAnswer::getContent);
            }
        }

        List<TenderNoticeAnswer> answers = this.list(queryWrapper);
        List<TenderNoticeAnswerListVO> voList = BeanCopierUtil.copyList(answers, TenderNoticeAnswerListVO.class);
        return voList;
    }

    @Override
    public boolean add(TenderNoticeAnswerVO tenderNoticeAnswerVO) {
        //新增招标公告答疑信息
        TenderNoticeAnswer answer = BeanCopierUtil.copyBean(tenderNoticeAnswerVO, TenderNoticeAnswer.class);
        answer.setQuestionUser(SecurityUtils.getUserId());

        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        answer.setVendorId(vendor.getId());
        answer.setVendorName(vendor.getEnterpriseName());
        return this.save(answer);
    }

    @Override
    public boolean edit(TenderNoticeAnswerVO tenderNoticeAnswerVO) {
        TenderNoticeAnswer answer = BeanCopierUtil.copyBean(tenderNoticeAnswerVO, TenderNoticeAnswer.class);
        answer.setAnswerUser(SecurityUtils.getUserId());
        return this.updateById(answer);
    }

}
