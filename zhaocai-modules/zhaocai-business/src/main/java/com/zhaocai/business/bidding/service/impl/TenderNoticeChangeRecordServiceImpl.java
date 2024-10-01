package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.domain.TenderNoticeAnswer;
import com.zhaocai.business.bidding.domain.TenderNoticeChangeRecord;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.TenderNoticeChangeRecordMapper;
import com.zhaocai.business.bidding.service.ITenderNoticeChangeRecordService;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeChangeRecordVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeChangeRecordQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeChangeRecordListVO;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 招标公告变更记录Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderNoticeChangeRecordServiceImpl extends ServiceImpl<TenderNoticeChangeRecordMapper,TenderNoticeChangeRecord> implements ITenderNoticeChangeRecordService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addNotice(TenderNoticeChangeRecordVO noticeChangeRecordVO) {

        //新增招标公告答疑信息
        TenderNoticeChangeRecord record = BeanCopierUtil.copyBean(noticeChangeRecordVO, TenderNoticeChangeRecord.class);
        /* 设置招标对应节点 */
        record.setNoticeStatus(TenderNoticeStatusEnum.TENDER_NOTICE.getState());
        return this.save(record);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(TenderNoticeChangeRecordVO noticeChangeRecordVO) {
        /*//如果是变更时间,删除所有其它得时间，只留下最新一条
        if (noticeChangeRecordVO.getType() == 1){
            this.remove(new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                    .eq(TenderNoticeChangeRecord::getNoticeId, noticeChangeRecordVO.getNoticeId())
                    .eq(TenderNoticeChangeRecord::getType, 1));
        }*/

        //新增招标公告答疑信息
        TenderNoticeChangeRecord record = BeanCopierUtil.copyBean(noticeChangeRecordVO, TenderNoticeChangeRecord.class);
        /* 设置招标对应节点 */
        record.setNoticeStatus(TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        return this.save(record);
    }

    @Override
    public List<TenderNoticeChangeRecordListVO> getListNotice(TenderNoticeChangeRecordQueryVO queryVO) {
        LambdaQueryWrapper<TenderNoticeChangeRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenderNoticeChangeRecord::getNoticeId, queryVO.getNoticeId());
        /* 招标对应节点 */
        if (queryVO.getNoticeStatus() != null){
            queryWrapper.eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState());
        }
        List<TenderNoticeChangeRecord> records = this.list(queryWrapper);
        List<TenderNoticeChangeRecordListVO> voList = new ArrayList<>();
        for (TenderNoticeChangeRecord record : records) {
            TenderNoticeChangeRecordListVO vo = BeanCopierUtil.copyBean(record, TenderNoticeChangeRecordListVO.class);
            vo.setTypeText(vo.getType() == 1 ? "变更时间" : "变更内容");
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public List<TenderNoticeChangeRecordListVO> getList(TenderNoticeChangeRecordQueryVO queryVO) {
        LambdaQueryWrapper<TenderNoticeChangeRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenderNoticeChangeRecord::getNoticeId, queryVO.getNoticeId());
        /* 招标对应节点 */
        queryWrapper.and(q -> q.eq(TenderNoticeChangeRecord::getNoticeStatus, null)
                .or().eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_ISSUE.getState()));
        List<TenderNoticeChangeRecord> records = this.list(queryWrapper);
        List<TenderNoticeChangeRecordListVO> voList = new ArrayList<>();
        for (TenderNoticeChangeRecord record : records) {
            TenderNoticeChangeRecordListVO vo = BeanCopierUtil.copyBean(record, TenderNoticeChangeRecordListVO.class);
            vo.setTypeText(vo.getType() == 1 ? "变更时间" : "变更内容");
            voList.add(vo);
        }
        return voList;
    }

}
