package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingOpenPeople;
import com.zhaocai.business.bidding.domain.BiddingResult;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.BiddingOpenPeopleMapper;
import com.zhaocai.business.bidding.service.IBiddingInfoService;
import com.zhaocai.business.bidding.service.IBiddingOpenPeopleService;
import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.BiddingOpenPeopleVO;
import com.zhaocai.business.bidding.vo.req.OpenBidVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingOpenPeopleQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingInfoListVO;
import com.zhaocai.business.bidding.vo.res.BiddingOpenPeopleListVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeSchemeInfoVO;
import com.zhaocai.business.common.enums.ApproveFlowPromptTemplateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowGroupEnum;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowModuleEnum;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.service.ThridPartyTodoTaskService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 开标人员信息Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class BiddingOpenPeopleServiceImpl extends ServiceImpl<BiddingOpenPeopleMapper,BiddingOpenPeople> implements IBiddingOpenPeopleService {

    @Autowired
    private ITenderNoticeService tenderNoticeService;
    @Autowired
    private IBiddingResultService biddingResultService;
    @Autowired
    private IBiddingInfoService biddingInfoService;
    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Autowired
    private ThridPartyTodoTaskService thridPartyTodoTaskService;
    @Autowired
    private RemoteUserService remoteuserservice;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(List<BiddingOpenPeopleVO> biddingOpenPeopleVos) {
        //新增开标人员信息
        if (CollectionUtils.isEmpty(biddingOpenPeopleVos) || biddingOpenPeopleVos.size() != NumberConstant.TWO){
            throw new ParamValidateException("开标人员需设置2人");
        }
        boolean res = false;
        Long noticeId = biddingOpenPeopleVos.get(0).getNoticeId();
        if (tenderNoticeService.getCountTenderNoticeStatus(
                noticeId, TenderNoticeStatusEnum.BID_OPENING.getState()) == 0){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }
        TenderNotice tenderNotice = tenderNoticeService.getOne(new LambdaUpdateWrapper<TenderNotice>()
                .eq(TenderNotice::getIsOpenPeople, NumberConstant.ONE)
                .eq(TenderNotice::getId, noticeId));
        if (!ObjectUtils.isEmpty(tenderNotice)){
            throw new ParamValidateException("已提交开标人员，不允许再设置开标人员");
        }

        this.remove(new LambdaUpdateWrapper<BiddingOpenPeople>()
                .eq(BiddingOpenPeople::getNoticeId, noticeId));
        for (BiddingOpenPeopleVO biddingOpenPeopleVo : biddingOpenPeopleVos) {
            BiddingOpenPeople openPeople = BeanCopierUtil.copyBean(biddingOpenPeopleVo, BiddingOpenPeople.class);
            res = this.save(openPeople);
        }

        /*ProcurementScheme procurementScheme = procurementSchemeService.getById(schemeId);
        //调第三方接口，生成开标人员的待办信息
        dealOpenPeopleTodoTask(openPeoples, procurementScheme);

        boolean resultData = true;
        if (resultData){
            //如果第三方待办生成成功，设置招标公告 '是否设置开标人员' 为已设置
            tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                    .set(TenderNotice::getIsOpenPeople, NumberConstant.ONE)
                    .eq(TenderNotice::getId, noticeId));
        }*/

        //todo su此处直接设置招标公告为 评标阶段，直接跳过开标阶段（用于演示）

//        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
//        Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
//        tenderNoticeService.updateStatus(noticeId, nextNoticeStatus);

        //todo su处理，理论上是要放到 openBid() 开标动作中做处理
        //处理额外环节逻辑
        /*if (TenderNoticeStatusEnum.RESULT_RELEASE.getState().equals(nextNoticeStatus) &&
                (detailVO.getSchemeType().equals(NumberConstant.FOUR))){
            //查看投标单信息（理论上投标单只能存在一条，因为只有一家供应商会投标）
            BiddingInfoQueryVO queryVO = new BiddingInfoQueryVO();
            queryVO.setNoticeId(noticeId);
            queryVO.setExcludeBiddingStatus(BiddingInfoStatusEnum.HAVE_ABANDON.getState());
            BiddingInfoListVO bidInfo = biddingInfoService.getList(queryVO).get(0);
            //生成定标数据
            genBiddingResultData(noticeId, schemeId, bidInfo);
        }*/

        return res;
    }

    @Override
    public boolean submit(Long noticeId) {
        List<BiddingOpenPeople> openPeoples = this.list(new LambdaQueryWrapper<BiddingOpenPeople>()
                .eq(BiddingOpenPeople::getNoticeId, noticeId));
        if (CollectionUtils.isEmpty(openPeoples)){
            throw new ParamValidateException("暂未获取到开标人员数据，请先设置开标人员");
        }
        Long schemeId = openPeoples.get(0).getSchemeId();

        ProcurementScheme procurementScheme = procurementSchemeService.getById(schemeId);
        //调第三方接口，生成开标人员的待办信息
        dealOpenPeopleTodoTask(openPeoples, procurementScheme);

        boolean res = tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getIsOpenPeople, NumberConstant.ONE)
                .eq(TenderNotice::getId, noticeId));
        return res;
    }


    private void dealOpenPeopleTodoTask(List<BiddingOpenPeople> openPeoples, ProcurementScheme procurementScheme){
        PushThirdPartyTodoTaskRequestDTO parentRequestDTO = new PushThirdPartyTodoTaskRequestDTO();
        List<PushThirdPartyTodoTaskSonRequestDTO> messageList = new ArrayList<>();

        for (BiddingOpenPeople openPeople : openPeoples) {
            PushThirdPartyTodoTaskSonRequestDTO requestDTO = new PushThirdPartyTodoTaskSonRequestDTO();
            requestDTO.setTitle("开标人员待办信息");
            requestDTO.setContent(String.format(ApproveFlowPromptTemplateEnum.BID_OPEN.getDesc(), procurementScheme.getProcurementSchemeName()));
            requestDTO.setArrivalTime(formatDate(new Date()));
            requestDTO.setCreateTime(formatDate(new Date()));
            String thridUserId = SecurityUtils.getThridUserId();
            requestDTO.setMsgFromPerCode(StringUtils.isNotEmpty(thridUserId) ? Long.parseLong(thridUserId) : null);
            requestDTO.setMsgFromPerName(SecurityUtils.getLoginUserNickName());
            String findThirdUserId = findThirdUserId(openPeople.getUserId());
            requestDTO.setMsgToPerCode(StringUtils.isNotEmpty(findThirdUserId) ? Long.parseLong(findThirdUserId) : null);
            requestDTO.setMsgToPerName(openPeople.getUserName());
            requestDTO.setFlowGroup(ThirdPartyTodoFlowGroupEnum.OPEN_BID.getDesc());
            requestDTO.setFlowModule(ThirdPartyTodoFlowModuleEnum.BID_MANAGE.getDesc());
            requestDTO.setFlowName(openPeople.getUserName() + "的" + ThirdPartyTodoFlowGroupEnum.OPEN_BID.getDesc());
            requestDTO.setDetailUrl(openPeople.getRedirectUrl());
//            requestDTO.setDetailUrl("/procurement/plan-detail/IjE4MTkyODk4NDM3Njk0NzA5Nzgi");
//            requestDTO.setUserObj("{\\\"id\\\":1111}");
//            requestDTO.setUserObj(openPeople.toString());
            //推送消息类型 1工作通知
            requestDTO.setType(NumberConstant.ONE);
            //推送公司类型 3晟晟
            requestDTO.setCompanyType(NumberConstant.THREE);
            messageList.add(requestDTO);
        }
        parentRequestDTO.setMessageList(messageList);
        parentRequestDTO.setAuthorization(SecurityUtils.getMasterControlToken());
        thridPartyTodoTaskService.pushTodoTask(parentRequestDTO);
    }

    private String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return sdf.format(date);
    }

    private String findThirdUserId(Long userId){
        R<SysUser> sysUser = remoteuserservice.selectUserInFoById(userId, SecurityConstants.INNER);
        if(sysUser.getCode() == HttpStatus.ERROR){
            throw new CheckedException("获取用户信息失败");
        }
        if (null != sysUser.getData()){
            return sysUser.getData().getThridUserId();
        }
        return null;
    }

    private void genBiddingResultData(Long noticeId, Long schemeId, BiddingInfoListVO bidInfo){
        BiddingResult biddingResult = new BiddingResult();
        biddingResult.setSchemeId(schemeId);
        biddingResult.setNoticeId(noticeId);
        biddingResult.setBiddingInfoId(bidInfo.getId());
        biddingResult.setVendorId(bidInfo.getVendorId());
        biddingResult.setVendorName(bidInfo.getVendorName());
        biddingResult.setContact(bidInfo.getContact());
        biddingResult.setPhone(bidInfo.getPhone());
        biddingResult.setRank(1);
        biddingResult.setCandidate("第1中标候选人");
        biddingResult.setBidResult(1);
        biddingResultService.save(biddingResult);
    }

    @Override
    public List<BiddingOpenPeopleListVO> getList(BiddingOpenPeopleQueryVO queryVO) {
        List<BiddingOpenPeopleListVO> listVO = new ArrayList<>();
        Long userId = SecurityUtils.getUserId();
        List<BiddingOpenPeople> openPeopleList = this.list(new LambdaQueryWrapper<BiddingOpenPeople>()
                .eq(BiddingOpenPeople::getNoticeId, queryVO.getNoticeId()));
        for (BiddingOpenPeople openPeople : openPeopleList) {
            BiddingOpenPeopleListVO vo = BeanCopierUtil.copyBean(openPeople, BiddingOpenPeopleListVO.class);
            vo.setIsOpenText(vo.getIsOpen() == 0 ? "未开标" : "已开标");

            if (null != userId){
                vo.setIsOpenUser(userId.equals(vo.getUserId()) ? NumberConstant.ONE : NumberConstant.ZERO);
            }

            listVO.add(vo);
        }
        return listVO;
    }

    @Override
    public List<BiddingOpenPeopleListVO> getTodoBiddingOpenList(BiddingOpenPeopleQueryVO queryVO) {
        List<BiddingOpenPeopleListVO> listVO = new ArrayList<>();
        LambdaQueryWrapper<BiddingOpenPeople> queryWrapper = new LambdaQueryWrapper<>();
        if (null != queryVO.getNoticeId()){
            queryWrapper.eq(BiddingOpenPeople::getNoticeId, queryVO.getNoticeId());
        }
        if (null != SecurityUtils.getUserId()){
            queryWrapper.eq(BiddingOpenPeople::getUserId, SecurityUtils.getUserId());
        }
        if (null != queryVO.getIsOpen()){
            queryWrapper.eq(BiddingOpenPeople::getIsOpen, queryVO.getIsOpen());
        }

        List<BiddingOpenPeople> openPeopleList = this.list(queryWrapper);
        for (BiddingOpenPeople openPeople : openPeopleList) {
            BiddingOpenPeopleListVO vo = BeanCopierUtil.copyBean(openPeople, BiddingOpenPeopleListVO.class);
            vo.setIsOpenText(vo.getIsOpen() == 0 ? "未开标" : "已开标");

            ProcurementScheme procurementScheme = procurementSchemeService.getById(openPeople.getSchemeId());
            if (!ObjectUtils.isEmpty(procurementScheme)){
                vo.setProcurementSchemeName(procurementScheme.getProcurementSchemeName());
                vo.setProcurementSchemeCode(procurementScheme.getProcurementSchemeCode());
                vo.setProcurementOfficerName(procurementScheme.getProcurementOfficerName());
            }

            listVO.add(vo);
        }
        return listVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean openBid(OpenBidVO openBidVO) {
        //根据采购方案id和用户id来更新当前 开标人员信息表状态
        BiddingOpenPeople openPeople = this.getOne(new LambdaQueryWrapper<BiddingOpenPeople>()
                .eq(null != openBidVO.getNoticeId(), BiddingOpenPeople::getNoticeId, openBidVO.getNoticeId())
                .eq(BiddingOpenPeople::getSchemeId, openBidVO.getSchemeId())
                .eq(BiddingOpenPeople::getUserId, SecurityUtils.getUserId()));
        if (ObjectUtils.isEmpty(openPeople)){
            throw new ParamValidateException("开标人员信息未查到，请刷新重试");
        }

        openPeople.setIsOpen(NumberConstant.ONE);
        boolean res = this.updateById(openPeople);

        //查询当前方案关联的开标人员，是否全部完成开标
        long count = this.count(new LambdaQueryWrapper<BiddingOpenPeople>()
                .eq(BiddingOpenPeople::getIsOpen, NumberConstant.ZERO)
                .eq(BiddingOpenPeople::getSchemeId, openBidVO.getSchemeId())
                .eq(BiddingOpenPeople::getNoticeId, openBidVO.getNoticeId()));
        if (count == 0L){
            //全部开标完成，改变招标阶段公告状态
            /*tenderNoticeService.updateStatus(
                    openPeople.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState());*/
            TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(openPeople.getNoticeId());
            Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
            tenderNoticeService.updateStatus(openPeople.getNoticeId(), nextNoticeStatus);
        }
        return res;
    }

}
