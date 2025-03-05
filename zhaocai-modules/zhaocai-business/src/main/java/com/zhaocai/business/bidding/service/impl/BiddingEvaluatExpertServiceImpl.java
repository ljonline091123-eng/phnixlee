package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingEvaluatExpert;
import com.zhaocai.business.bidding.domain.BiddingOpenPeople;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.BiddingEvaluatExpertMapper;
import com.zhaocai.business.bidding.service.IBiddingEvaluatExpertService;
import com.zhaocai.business.bidding.vo.req.BiddingEvaluatExpertVO;
import com.zhaocai.business.bidding.vo.req.BiddingExpertListVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingEvaluatExpertQueryVO;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.BiddingEvaluatExpertListVO;
import com.zhaocai.business.bidding.vo.res.EvalTaskPageListVO;
import com.zhaocai.business.common.enums.ApproveFlowPromptTemplateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowGroupEnum;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowModuleEnum;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.service.ThridPartyTodoTaskService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 评标专家人员信息Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class BiddingEvaluatExpertServiceImpl extends ServiceImpl<BiddingEvaluatExpertMapper,BiddingEvaluatExpert> implements IBiddingEvaluatExpertService {

    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Autowired
    private IExpertService expertService;
    @Autowired
    private IMinProjectService minProjectService;
    @Autowired
    private ThridPartyTodoTaskService thridPartyTodoTaskService;
    @Autowired
    private RemoteUserService remoteuserservice;

    @Override
    public boolean add(BiddingEvaluatExpertVO biddingEvaluatExpertVO) {
        verifyParam(biddingEvaluatExpertVO);

        long dataCount = this.count(new LambdaQueryWrapper<BiddingEvaluatExpert>()
                        .eq(BiddingEvaluatExpert::getNoticeId, biddingEvaluatExpertVO.getNoticeId()));
        if (dataCount > 0){
            throw new ParamValidateException("已设置评标专家，请勿重复操作");
        }

        List<BiddingExpertListVO> expertListVO = biddingEvaluatExpertVO.getExpertListVO();
        List<BiddingExpertListVO> notJoinExpertListVO = biddingEvaluatExpertVO.getNotJoinExpertListVO();
        List<BiddingEvaluatExpert> evaluatExperts = new ArrayList<>();
        for (BiddingExpertListVO biddingExpertListVO : expertListVO) {

            //查询当前设置的专家是否已经有待评标任务了，如果有，则不允许设置，需求来源：张贵荣
            EvalTaskPageVO pageVO = new EvalTaskPageVO();
            pageVO.setExpertId(biddingExpertListVO.getExpertId());
            pageVO.setNoticeStatus(TenderNoticeStatusEnum.EVALUATION_BID.getState());
            PageResult<EvalTaskPageListVO> pageResult = this.getEvalSchemaPage(pageVO);
            if (null != pageResult.getTotal() && pageResult.getTotal() > 0){
                throw new ParamValidateException(biddingExpertListVO.getExpertName() + "专家还有为未完成的评标任务、不允许选择");
            }

            long count = this.count(new LambdaQueryWrapper<BiddingEvaluatExpert>()
                    .eq(BiddingEvaluatExpert::getNoticeId, biddingEvaluatExpertVO.getNoticeId())
                    .eq(BiddingEvaluatExpert::getExpertId, biddingExpertListVO.getExpertId()));
            if (count > 0){
                continue;
            }

            BiddingEvaluatExpert evaluatExpert = BeanCopierUtil.copyBean(biddingExpertListVO, BiddingEvaluatExpert.class);
            evaluatExpert.setNoticeId(biddingEvaluatExpertVO.getNoticeId());
            evaluatExpert.setSchemeId(biddingEvaluatExpertVO.getSchemeId());
            //此段逻辑不用（设置专家是否需要评标，如果评分方法为‘最低评标价法’，专家则不需评标）
//            evaluatExpert.setIsEval(biddingEvaluatExpertVO.getEvalWay() == NumberConstant.ONE ? NumberConstant.ONE : NumberConstant.ZERO);
            evaluatExpert.setIsEval(NumberConstant.ZERO);
            evaluatExpert.setIsJoin(NumberConstant.ONE);
            evaluatExperts.add(evaluatExpert);
        }
        for (BiddingExpertListVO notJoinBiddingExpertListVO : notJoinExpertListVO) {
            BiddingEvaluatExpert notJoinEvaluatExpert = BeanCopierUtil.copyBean(notJoinBiddingExpertListVO, BiddingEvaluatExpert.class);
            notJoinEvaluatExpert.setNoticeId(biddingEvaluatExpertVO.getNoticeId());
            notJoinEvaluatExpert.setSchemeId(biddingEvaluatExpertVO.getSchemeId());
            notJoinEvaluatExpert.setIsEval(NumberConstant.ZERO);
            notJoinEvaluatExpert.setIsJoin(NumberConstant.TWO);
            evaluatExperts.add(notJoinEvaluatExpert);
        }
        if (evaluatExperts.size() > 0){
            //新增评标专家人员信息
            boolean res = this.saveBatch(evaluatExperts, evaluatExperts.size());
        }

        //调第三方接口，生成评标专家的待办信息
        ProcurementScheme procurementScheme = procurementSchemeService.getById(biddingEvaluatExpertVO.getSchemeId());
        dealExpertEvalTodoTask(expertListVO, procurementScheme);

        return true;
    }

    private void dealExpertEvalTodoTask(List<BiddingExpertListVO> expertListVO, ProcurementScheme procurementScheme){
        PushThirdPartyTodoTaskRequestDTO parentRequestDTO = new PushThirdPartyTodoTaskRequestDTO();
        List<PushThirdPartyTodoTaskSonRequestDTO> messageList = new ArrayList<>();

        MinProjectVO project = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());

        for (BiddingExpertListVO biddingExpertListVO : expertListVO) {
            Expert expert = expertService.getById(biddingExpertListVO.getExpertId());

            PushThirdPartyTodoTaskSonRequestDTO requestDTO = new PushThirdPartyTodoTaskSonRequestDTO();
            requestDTO.setTitle("评标人员待办信息");
            requestDTO.setPrjName((project==null?"":project.getMinAccountSimpleName()==null?"":project.getMinAccountSimpleName()));
            requestDTO.setContent(String.format(ApproveFlowPromptTemplateEnum.BID_EVAL.getDesc(), procurementScheme.getProcurementSchemeName()));
            requestDTO.setArrivalTime(formatDate(new Date()));
            requestDTO.setCreateTime(formatDate(new Date()));
            String thridUserId = SecurityUtils.getThridUserId();
            requestDTO.setMsgFromPerCode(StringUtils.isNotEmpty(thridUserId) ? Long.parseLong(thridUserId) : null);
            requestDTO.setMsgFromPerName(SecurityUtils.getLoginUserNickName());
            String findThirdUserId = findThirdUserId(expert.getUserId());
            requestDTO.setMsgToPerCode(StringUtils.isNotEmpty(findThirdUserId) ? Long.parseLong(findThirdUserId) : null);
            requestDTO.setMsgToPerName(expert.getExpertName());
            requestDTO.setFlowGroup(ThirdPartyTodoFlowGroupEnum.EXPERT_EVAL.getDesc());
            requestDTO.setFlowModule(ThirdPartyTodoFlowModuleEnum.BID_MANAGE.getDesc());
            requestDTO.setFlowName(expert.getExpertName() + "的" + ThirdPartyTodoFlowGroupEnum.EXPERT_EVAL.getDesc());
            requestDTO.setDetailUrl(biddingExpertListVO.getRedirectUrl());
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

    @Override
    public List<BiddingEvaluatExpertListVO> getList(BiddingEvaluatExpertQueryVO queryVO) {
        LambdaQueryWrapper<BiddingEvaluatExpert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BiddingEvaluatExpert::getNoticeId, queryVO.getNoticeId());
        queryWrapper.eq(BiddingEvaluatExpert::getSchemeId, queryVO.getSchemeId());
        if (StringUtils.isNotEmpty(queryVO.getExpertName())){
            queryWrapper.like(BiddingEvaluatExpert::getExpertName, queryVO.getExpertName());
        }
        List<BiddingEvaluatExpert> evaluatExperts = this.list(queryWrapper);
        List<BiddingEvaluatExpertListVO> list = BeanCopierUtil.copyList(evaluatExperts, BiddingEvaluatExpertListVO.class);
        return list;
    }

    @Override
    public PageResult<EvalTaskPageListVO> getEvalSchemaPage(EvalTaskPageVO pageVO) {
        IPage<EvalTaskPageListVO> iPage = baseMapper.findEvalSchemaPage(pageVO.toMybatisPage(), pageVO);
//        iPage.getRecords().forEach(item -> {
//            item.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(item.getNoticeStatus()));
//        });
        return new PageResult<>(iPage);
    }

    /**
     * 校验参数
     * */
    private void verifyParam(BiddingEvaluatExpertVO biddingEvaluatExpertVO){
        List<BiddingExpertListVO> expertListVO = biddingEvaluatExpertVO.getExpertListVO();
        if (CollectionUtils.isEmpty(expertListVO) || expertListVO.size() < NumberConstant.FIVE || isEven(expertListVO.size())){
            throw new ParamValidateException("专家人数设置需不少于5人，且为奇数");
        }

    }

    /**
     * 判断数字是奇数还是偶数的方法
     * */
    public static boolean isEven(int number) {
        return number % 2 == 0;
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

}
