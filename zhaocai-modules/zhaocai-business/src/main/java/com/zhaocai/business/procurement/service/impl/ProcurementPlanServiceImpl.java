package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.res.ContractPlanningNoticeVO;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowGroupEnum;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowModuleEnum;
import com.zhaocai.business.manager.http.dto.req.ContractPlanMaterialListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.dto.req.UsersRoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleContractPlanListResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleListResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.manager.http.service.PlatRoleService;
import com.zhaocai.business.manager.http.service.ThridPartyTodoTaskService;
import com.zhaocai.business.procurement.domain.*;
import com.zhaocai.business.procurement.dto.ContractProcurementPlanDTO;
import com.zhaocai.business.procurement.dto.SubjectMatterDTO;
import com.zhaocai.business.procurement.mapper.ProcurementPlanMapper;
import com.zhaocai.business.procurement.service.*;
import com.zhaocai.business.procurement.vo.req.*;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAreaDivisionService;
import com.zhaocai.business.pub.service.IBusinessCodeService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.common.signature.domain.AgreementSignature;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import com.zhaocai.system.api.domain.SetConfigValueDTO;
import com.zhaocai.system.api.system.RemoteSystemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 采购计划Service业务层处理
 *
 * @author chenming
 * @date 2024-05-24
 */
@Slf4j
@Service
public class ProcurementPlanServiceImpl extends ServiceImpl<ProcurementPlanMapper,ProcurementPlan> implements IProcurementPlanService {
    @Autowired
    private IBusinessCodeService businessCodeService;

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    @Autowired
    private IMaterialsListService materialsListService;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Autowired
    private ContractPlanService contractPlanService;

    @Autowired
    private IAreaDivisionService areaDivisionService;

    @Autowired
    private IProcurementSchemePlanRelateService procurementSchemePlanRelateService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    /** 合约是否可拆分标识 */
    private final String CONFIG_CONTRACT_SPLIT_FLAG = "contract.split.flag";

    @Autowired
    private ThridPartyTodoTaskService thridPartyTodoTaskService;

    @Autowired
    private IContractPlanningPushRecordService contractPlanningPushRecordService;

    @Autowired
    private PlatRoleService platRoleService;

    @Autowired
    private ITenderNoticeService tenderNoticeService;

    @Lazy
    @Autowired
    private IMinProjectService minProjectService;

    @Lazy
    @Autowired
    private IProcurementPlanService procurementPlanService;

    @Override
    public PageResult<ProcurementPlanListVO> listPage(ProcurementPlanListQueryVO queryVO) {
        IPage<ProcurementPlanListVO> iPage =  baseMapper.selectListPage(queryVO.toMybatisPage(), queryVO);
        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<ContractPlanningListVO> listContractPlanningPage(ContractPlanningListQueryVO queryVO) {
        return contractPlanService.getContractPlanningList(queryVO);
    }

    @Override
    public ContractPlanningVO listContractPlanning(ContractPlanningListQueryVO queryVO) {
        PageResult<ContractPlanningListVO> contractPlanningList = contractPlanService.getContractPlanningList(queryVO);

        // 1. 处理状态
        for (ContractPlanningListVO contractPlanning : contractPlanningList.getRows()) {
            if (contractPlanning.getPlanningBalance() == null || contractPlanning.getPlanningBalance().compareTo(BigDecimal.ZERO) == 0) {
                // 规划余额等于 0  --> 已完成
                contractPlanning.setBiddingState("3");
            } else if (contractPlanning.getPlannedAmountInclTax().compareTo(contractPlanning.getIncurredPlannedAmount()) > 0) {
                // 规划金额 > 已发生规划金额  --> 进行中
                contractPlanning.setBiddingState("2");
            } else {
                contractPlanning.setBiddingState("1");
            }
        }

        // 2. 再过滤
        Predicate<ContractPlanningListVO> predicate = contractPlanningListVO -> true;
        if (StringUtils.isNotBlank(queryVO.getBiddingMethodCode())) {
            predicate = predicate.and(listVO -> queryVO.getBiddingMethodCode().equals(listVO.getBiddingMethodCode()));
        }
        if (StringUtils.isNotBlank(queryVO.getBidResponsibleOrg())) {
            predicate = predicate.and(listVO -> queryVO.getBidResponsibleOrg().equals(listVO.getBidResponsibleOrg()));
        }
        if (StringUtils.isNotBlank(queryVO.getBiddingState())) {
            predicate = predicate.and(listVO -> queryVO.getBiddingState().equals(listVO.getBiddingState()));
        }
        List<ContractPlanningListVO> resultList = contractPlanningList.getRows().stream()
                .filter(predicate)
                .collect(Collectors.toList());

        // 3. 处理数据
        BigDecimal totalPlannedAmountInclTax = BigDecimal.ZERO;
        BigDecimal totalIncurredPlannedAmount = BigDecimal.ZERO;
        BigDecimal totalPlanningBalance = BigDecimal.ZERO;

        if (CollectionUtil.isNotEmpty(resultList)) {
            // 获取合约规划的第一个采购计划
            List<String> contractIdList = resultList.stream()
                    .map(ContractPlanningListVO::getContractPlanningId)
                    .collect(Collectors.toList());
            List<ContractProcurementPlanDTO> firstTimeProcurementPlanList = baseMapper.selectFirstTimeProcurementPlanByContractPlan(contractIdList);
            Map<String,ContractProcurementPlanDTO> firstTimeProcurementPlanMap = new HashMap<>();
            if (CollectionUtil.isNotEmpty(firstTimeProcurementPlanList)) {
                firstTimeProcurementPlanMap = firstTimeProcurementPlanList.stream()
                        .collect(Collectors.toMap(ContractProcurementPlanDTO::getContractPlanningId,val -> val));
            }

            /* 查询推送记录 */
            List<ContractPlanningPushRecord> records = contractPlanningPushRecordService.getByCondition(contractIdList);
            /* 查询单条 */
            Map<String, ContractPlanningPushRecord> recordMap = records.stream().collect(Collectors.toMap(ContractPlanningPushRecord::getContractPlanningId, Function.identity()));
            /* 查询招标集合 */
            Map<String, List<ContractPlanningPushRecord>> recordMapList = records.stream().collect(Collectors.groupingBy(ContractPlanningPushRecord::getContractPlanningId));
            /* 根据合约规划id集合查询对应的招标对象数据和采购方案数据 */
            List<ContractPlanningNoticeVO> recordsQuery = tenderNoticeService.getListByContractPlanningId(new ContractPlanningQueryVO(contractIdList));
            Map<String, List<ContractPlanningNoticeVO>> recordMapQuery = recordsQuery.stream().collect(Collectors.groupingBy(ContractPlanningNoticeVO::getContractPlanningId));

            for (ContractPlanningListVO contractPlanning :resultList) {
                totalPlannedAmountInclTax = NumberUtil.add(totalPlannedAmountInclTax,contractPlanning.getPlannedAmountInclTax());
                totalIncurredPlannedAmount = NumberUtil.add(totalIncurredPlannedAmount,contractPlanning.getIncurredPlannedAmount());
                totalPlanningBalance = NumberUtil.add(totalPlanningBalance,contractPlanning.getPlanningBalance());

                ContractProcurementPlanDTO contractProcurementPlan = firstTimeProcurementPlanMap.get(contractPlanning.getContractPlanningId());
                if (contractProcurementPlan != null) {
                    contractPlanning.setEnterIntoTime(contractProcurementPlan.getArrivalDate());
                }

                /* 推送状态 细分到合约规划 */
                ContractPlanningPushRecord contractPlanningPushRecord = recordMap.get(contractPlanning.getContractPlanningId());
                if (ObjectUtil.isNotEmpty(contractPlanningPushRecord)){
                    contractPlanning.setPushStatus(contractPlanningPushRecord.getPushStatus());
                }

                /* 增加推送状态 细分到投标对象 */
                if (recordMapList!=null && !recordMapList.isEmpty()){
                    if (recordMapQuery!=null && !recordMapQuery.isEmpty()){
                        List<ContractPlanningPushRecord> contractPlanningPushRecordList = recordMapList.get(contractPlanning.getContractPlanningId());
                        List<ContractPlanningNoticeVO> contractPlanningPushList = recordMapQuery.get(contractPlanning.getContractPlanningId());
                        if (contractPlanningPushRecordList!=null && !contractPlanningPushRecordList.isEmpty()){
                            if (contractPlanningPushList!=null && !contractPlanningPushList.isEmpty()){
                                /* 默认都是未推送 */
                                contractPlanningPushList.forEach(obj -> obj.setPushStatus(0));
                                /* 获取已推送的 采购方案id */
                                Set<Long> schemeIds = contractPlanningPushRecordList.stream()
                                        .map(ContractPlanningPushRecord::getSchemeId)
                                        .collect(Collectors.toSet());
                                /* 对比已推送的采购方案id,并设置值 */
                                contractPlanningPushList.stream()
                                        .filter(obj -> schemeIds.contains(obj.getSchemeId()))
                                        .forEach(obj -> obj.setPushStatus(1));
                                /* 添加到返回对象中 */
                                contractPlanning.setContractPlanningNoticeVOList(contractPlanningPushList);
                            }
                        }
                    }
                }

            }
        }

        // 排序
        resultList.sort(Comparator.comparing(ContractPlanningListVO::getBiddingTime));

        ContractPlanningVO contractPlanning = new ContractPlanningVO();
        contractPlanning.setContractPlanningList(resultList);
        contractPlanning.setTotalSize(resultList.size());
        contractPlanning.setTotalPlannedAmountInclTax(totalPlannedAmountInclTax);
        contractPlanning.setTotalIncurredPlannedAmount(totalIncurredPlannedAmount);
        contractPlanning.setTotalPlanningBalance(totalPlanningBalance);

        return contractPlanning;
    }

    @Override
    public ContractPlanMaterialListVO listContractMaterials(ContractPlanMaterialListQueryVO queryVO) {
        List<ContractMaterialsListVO> materialsList = contractPlanService.getContractMaterialsList(queryVO);

        // 计算上限价
        BigDecimal upperLimitPrice = BigDecimal.ZERO;
        SubjectMatterDTO subjectMatter;
        for(ContractMaterialsListVO listVO : materialsList){
            upperLimitPrice = NumberUtil.add(upperLimitPrice, AmountCalUtil.calTotalAmountInclTax(listVO.getCount(),listVO.getUnitPriceInclTax()));

            subjectMatter = materialsListService.getSubjectMatter(queryVO.getProcurementType(),listVO.getMaterialsCode(),queryVO.getConPlanCode());
            if (subjectMatter != null) {
                listVO.setSubjectMatterCode(subjectMatter.getSubjectMatterCode());
                listVO.setSubjectMatterName(subjectMatter.getSubjectMatterName());
                // 交易标的物标识 ，为 1 的时候前端需要自己选择
                if (Constants.SUBJECT_MATTER_BLANK.equals(subjectMatter.getSubjectMatterCode())) {
                    listVO.setSubjectMatterFlag("1");
                } else {
                    listVO.setSubjectMatterFlag("0");
                }
            }
        }

        ContractPlanMaterialListVO contractPlanMaterialLis = new ContractPlanMaterialListVO(upperLimitPrice,materialsList);

        // 交易标的物名称
        String subjectMatterText = materialsList.stream()
                        .map(ContractMaterialsListVO::getSubjectMatterName)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .collect(Collectors.joining(","));
        contractPlanMaterialLis.setSubjectMatterText(subjectMatterText);

        // 交易标的物编码
        String subjectMatterCode = materialsList.stream()
                .map(ContractMaterialsListVO::getSubjectMatterCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
        contractPlanMaterialLis.setSubjectMatterCode(subjectMatterCode);

        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(queryVO.getProcurementType())) {
            //购买材料需要确认交易标的物值   0其它 1 钢筋采购 2 商品砼采购
            contractPlanMaterialLis.setSubjectMatter(materialsListService.getSubjectMatterType(subjectMatterCode));
        }

        return contractPlanMaterialLis;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public Long saveProcurementPlan(ProcurementPlanRequestVO requestVO) {
        checkMaterialsList(requestVO);
        if (NumberUtil.isNullOrZero(requestVO.getProcurementPlan().getId())) {
            // 新增
            addProcurementPlan(requestVO);
        } else {
            // 修改
            updateProcurementPlan(requestVO);
        }

        return requestVO.getProcurementPlan().getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void submitProcurementPlan(Long planId) {
        ProcurementPlan procurementPlan = this.getById(planId);
        ValidateUtils.isNullException(procurementPlan,"该采购计划不存在，请确认");
        ValidateUtils.validateStatusNotEquals(ProcurementPlanStateEnum.DRAFT::equalsState,procurementPlan.getState(),"该状态下的采购计划不允许提及");

        super.update(new LambdaUpdateWrapper<ProcurementPlan>()
                .set(ProcurementPlan::getState, ProcurementPlanStateEnum.SUBMITTED.getState())
                .eq(ProcurementPlan::getId,planId));

        // 拆分结果回写至商务策划
        updatePlanQuantityAmount(planId,Constants.CONTRACT_UPDATE_ADD_FLAG);
    }

    @Override
    public ProcurementPlanDetailVO getProcurementPlanDetail(Long id) {
        ProcurementPlan procurementPlan = this.getById(id);
        ValidateUtils.isNullException(procurementPlan,"该采购计划不存在，请确认");

        // 采购计划详情
        ProcurementPlanVO procurementPlanVO = BeanCopierUtil.copyBean(procurementPlan,ProcurementPlanVO.class);

        // 设置省、市名称
        List<String> regionCodeList = Arrays.asList(procurementPlanVO.getRegionProvinceCode(),procurementPlanVO.getRegionCityCode());

        Map<String,String> regionMap = areaDivisionService.getAreaDivisionMap(regionCodeList);
        procurementPlanVO.setRegionProvinceName(regionMap.get(procurementPlanVO.getRegionProvinceCode()) == null ? "" : regionMap.get(procurementPlanVO.getRegionProvinceCode()));
        procurementPlanVO.setRegionCityName(regionMap.get(procurementPlanVO.getRegionCityCode()) == null ? "" : regionMap.get(procurementPlanVO.getRegionCityCode()));

        // 获取采购清单
        List<ContractSplitMaterialsVO> splitMaterials = materialsListService.listMaterialsByPlanId(id,false);

        // 获取合约规划详情
        ContractPlanningListVO contractPlanning = contractPlanningService.getByProcurementIdFromUnderling(id);
        procurementPlanVO.setProjectCode(contractPlanning.getProjectCode());
        procurementPlanVO.setProjectName(contractPlanning.getProjectName());

        return ProcurementPlanDetailVO.builder()
                .procurementPlan(procurementPlanVO)
                .splitMaterials(splitMaterials)
                .contractPlanning(contractPlanning)
                .build();
    }


    @Override
    public List<ContractSplitListVO> listContractSplit(Long id) {
        return contractPlanningSplitService.listContractSplitByPlanId(id);
    }

    @Override
    public ProcurementPlan getBySplitId(Long splitId) {
        return baseMapper.selectBySplitId(splitId);
    }

    @Override
    public PageResult<ProcurementPlanContractSplitVO> listPlanContractSplit(ProcurementPlanContractSplitQueryVO queryVO) {
        return contractPlanningSplitService.listPlanContractSplit(queryVO);
    }

    @Override
    public void cancellationProcurementPlan(Long planId) {
        ProcurementPlan procurementPlan = this.getById(planId);
        ValidateUtils.isNullException(procurementPlan,"该采购计划不存在，请确认");

        // 判断该采购计划被哪些采购方案引用
        List<ProcurementScheme> relateSchemeList = procurementSchemePlanRelateService.listProcurementSchemeByRelatePlanId(planId);
        if (CollectionUtil.isNotEmpty(relateSchemeList)) {
            String schemeName = relateSchemeList.stream()
                    .filter(val -> !ProcurementSchemeStateEnum.CANCELLATION.equalsState(val.getState()))
                    .map(ProcurementScheme::getProcurementSchemeName)
                    .distinct()
                    .collect(Collectors.joining(","));
            if (StringUtils.isNotBlank(schemeName)) {
                throw new ParamValidateException("该采购计划已被采购方案引用，请重新确认。引用的采购方案有:" + schemeName);
            }
        }

        if (ProcurementPlanStateEnum.SUBMITTED.equalsState(procurementPlan.getState())) {
            // 如果是已提交的采购方案，则需要回收商务策划的数据
            updatePlanQuantityAmount(planId,Constants.CONTRACT_UPDATE_REDUCE_FLAG);
        }

        super.update(new LambdaUpdateWrapper<ProcurementPlan>()
                .set(ProcurementPlan::getState, ProcurementPlanStateEnum.CANCELLATION.getState())
                .eq(ProcurementPlan::getId,planId));
    }

    @Override
    public void pushProcurementPlan(ProcurementPlanPushVO planPushVO) {
        //设置第三方数据合约规划推送记录
        savaContractPlanningPushRecord(planPushVO);
        //调第三方接口，生成采购计划的待办信息
        dealOpenPeopleTodoTask(planPushVO);
    }

    @Override
    public UsersRoleContractPlanListResponseDTO getUsersRoleContractPlanList(ContractPlanningQueryVO requestDTO) {
        /* 请求获取第三方用户数据 */
        List<UsersRoleListResponseDTO> userList = platRoleService.getUsersRoleList(new UsersRoleListRequestDTO());
        /* 根据合约规划id或者code查询对应的招标对象数据和采购方案数据 */
        List<ContractPlanningNoticeVO> contractPlanningNoticeVOList = tenderNoticeService.getListByContractPlanningId(requestDTO);
        /* 用来存储该方法返回对象数据 */
        UsersRoleContractPlanListResponseDTO userContract = new UsersRoleContractPlanListResponseDTO();
        if(userList!=null && !userList.isEmpty()){
            userContract.setUserList(userList);
        }
        if(contractPlanningNoticeVOList!=null && !contractPlanningNoticeVOList.isEmpty()){
            userContract.setContractPlanningNoticeVOList(contractPlanningNoticeVOList);
        }
        return userContract;
    }

    public void savaContractPlanningPushRecord(ProcurementPlanPushVO planPushVO){
        ContractPlanningPushRecord record = new ContractPlanningPushRecord();
        record.setContractPlanningId(planPushVO.getContractPlanningId());
        record.setContractPlanningCode(planPushVO.getContractPlanningCode());
        /* 多增加 采购方案 招标对象 记录 */
        record.setProcurementSchemeCode(planPushVO.getProcurementSchemeCode());
        record.setNoticeId(planPushVO.getNoticeId());
        record.setSchemeId(planPushVO.getSchemeId());
        //JSONObject.parseArray("从数据库中取出的String类型的字段",T.class);
        record.setPushObj(JSONObject.toJSONString(planPushVO.getUserList()));
        record.setPushStatus(NumberConstant.ONE);
        contractPlanningPushRecordService.save(record);
    }

    /* 推送合约规划 */
    public void dealOpenPeopleTodoTask (ProcurementPlanPushVO planPushVO){
        PushThirdPartyTodoTaskRequestDTO parentRequestDTO = new PushThirdPartyTodoTaskRequestDTO();
        List<PushThirdPartyTodoTaskSonRequestDTO> messageList = new ArrayList<>();
        String nowTime = formatDate(new Date());
        String nickName = SecurityUtils.getLoginUserNickName();
        Long thridUserId = StringUtils.isNotEmpty(SecurityUtils.getThridUserId()) ? Long.parseLong(SecurityUtils.getThridUserId()) : null;



        for (ProcurementPlanPushUserVO userData : planPushVO.getUserList()){
            PushThirdPartyTodoTaskSonRequestDTO requestDTO = new PushThirdPartyTodoTaskSonRequestDTO();
            requestDTO.setTitle("采购计划待办信息");
//            requestDTO.setContent(String.format(ApproveFlowPromptTemplateEnum.PROCUREMENT_PLAN_PUSH.getDesc(), planPushVO.getContractPlanningName()));

            /* 获取已有的合约规划 */
            ContractPlanning contractPlanning = contractPlanningService.getOne(new LambdaQueryWrapper<ContractPlanning>()
                    .eq(ContractPlanning::getContractPlanningCode,planPushVO.getContractPlanningCode()));
            /* 查询采购计划 */
            ProcurementPlan procurementPlan = null;
            if(contractPlanning!=null&&contractPlanning.getPlanId()!=null){
                procurementPlan = procurementPlanService.getById(contractPlanning.getPlanId());
            }
            String content = String.format(ApproveFlowPromptTemplateEnum.PROCUREMENT_PLAN_NOTICE_PUSH.getDesc(),
                    SecurityUtils.getLoginUserNickName(),/* 推送人 登录人 */
                    contractPlanning==null?"":contractPlanning.getProjectName(),/* 项目名称 */
                    contractPlanning==null?"":contractPlanning.getContractPlanningCategoryName(),/* 类型 */
                    contractPlanning==null?"":contractPlanning.getContractPlanningName(),/* 合约规划名称 */
                    procurementPlan==null?"":procurementPlan.getProcurementPlanName(),/* 采购计划名称 */
                    planPushVO.getBiddingTime()==null?"":planPushVO.getBiddingTime(),/* 招标时间 */
                    planPushVO.getEnterIntoTime()==null?"":planPushVO.getEnterIntoTime(),/* 进场时间 */
                    procurementPlan==null?"":procurementPlan.getProcurementOfficerName(),/* 采购经办人名称 */
                    (procurementPlan==null?"":procurementPlan.getRegionProvinceCode()) + (procurementPlan==null?"":procurementPlan.getRegionCityCode())/* 省 + 市 */
            );
            requestDTO.setContent(content);/* 推送内容 */

            requestDTO.setArrivalTime(nowTime);
            requestDTO.setCreateTime(nowTime);
            requestDTO.setMsgFromPerCode(thridUserId);
            requestDTO.setMsgFromPerName(nickName);
            requestDTO.setMsgToPerCode(userData.getUserId());
            requestDTO.setMsgToPerName(userData.getNickName());
            requestDTO.setFlowGroup(ThirdPartyTodoFlowGroupEnum.START_PROCUREMENT_PLAN.getDesc());
            requestDTO.setFlowModule(ThirdPartyTodoFlowModuleEnum.PROCUREMENT_PLAN.getDesc());
            requestDTO.setFlowName(userData.getNickName() + "的" + ThirdPartyTodoFlowGroupEnum.START_PROCUREMENT_PLAN.getDesc());
            requestDTO.setDetailUrl(planPushVO.getRedirectUrl());
//            requestDTO.setUserObj("");
            //推送消息类型 1工作通知
            requestDTO.setType(NumberConstant.ONE);
            //推送公司类型 3晟晟
            requestDTO.setCompanyType(NumberConstant.THREE);
            messageList.add(requestDTO);
        }
        parentRequestDTO.setMessageList(messageList);
        parentRequestDTO.setAuthorization(SecurityUtils.getMasterControlToken());
//        parentRequestDTO.setAuthorization("Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjA3YzRiZGQyLTczMTUtNGNkMC04YTAxLTEwMWNhODAxYjdhNl8xODE2MzY1MTczMyJ9.W6eLb5HmdglRIO2sle2O1sEm2ns4cJT_VNNTi-kMK9VpQIZZqs6gHF0n329G6EdCxkWjqXMGmzaYlLLxAQUbkw");
        thridPartyTodoTaskService.pushTodoTask(parentRequestDTO);
    }

    @Override
    public void setContractPlanSplitFlag(String flag) {
        SetConfigValueDTO setConfigValue = new SetConfigValueDTO(CONFIG_CONTRACT_SPLIT_FLAG,flag);
        remoteSystemService.setConfigValueByKey(setConfigValue, SecurityConstants.INNER);
    }

    @Override
    public String getContractPlanSplitFlag() {
        String splitFlag = remoteSystemService.configKeyStr(CONFIG_CONTRACT_SPLIT_FLAG);
        return StringUtils.isNotBlank(splitFlag) ? splitFlag : "0";
    }

    /**
     * 修改采购计划
     * @param requestVO
     */
    private void updateProcurementPlan(ProcurementPlanRequestVO requestVO) {
        ProcurementPlan procurementPlan = requestVO.getProcurementPlan();
        ProcurementPlan checkPlan = baseMapper.selectById(procurementPlan.getId());

        ValidateUtils.isNullException(checkPlan,"该采购计划不存在");
        ValidateUtils.validateStatusEquals(ProcurementPlanStateEnum.SUBMITTED::equalsState,checkPlan.getState(),"已提交的采购计划不能再做修改");
        ValidateUtils.validateStatusEquals(ProcurementPlanStateEnum.CANCELLATION::equalsState,checkPlan.getState(),"已作废的采购计划不能再做修改");
        ValidateUtils.validateStatusNotEquals(checkPlan.getCreateId()::equals,SecurityUtils.getUserId(),"您无权修改不属于您的采购计划");

        // 修改采购计划
        baseMapper.updateById(procurementPlan);

        // 修改合约拆分和物料信息
        contractPlanningSplitService.updateContractPlanningSplit(requestVO.getSplitRequestList(),procurementPlan.getId(),procurementPlan.getPriceType());

        // 重新保存合约规划
        contractPlanningService.updateContractPlanning(requestVO.getContractPlanning(),procurementPlan.getId());
    }

    /**
     * 新增采购计划
     * @param requestVO
     */
    private void addProcurementPlan(ProcurementPlanRequestVO requestVO) {
        ProcurementPlan procurementPlan = requestVO.getProcurementPlan();
        procurementPlan.setProcurementPlanCode(getProcurementPlanCode());
        procurementPlan.setProcurementReporter(SecurityUtils.getUserId());
        procurementPlan.setProcurementReporterName(SecurityUtils.getLoginUser().getSysUser().getNickName());
        procurementPlan.setState(ProcurementPlanStateEnum.DRAFT.getState());

        baseMapper.insert(procurementPlan);

        // 保存合约拆分和物料信息
        contractPlanningSplitService.saveContractPlanningSplit(requestVO.getSplitRequestList(),procurementPlan.getId(),procurementPlan.getPriceType());

        // 保存合约规划
        contractPlanningService.addContractPlanning(requestVO.getContractPlanning(),procurementPlan.getId());
    }

    /**
     * 获取采购计划编码
     * @return
     */
    private String getProcurementPlanCode() {
        return "CGFJH" + LocalDate.now().getYear() + businessCodeService.getBusinessCode(BusinessCodeEnum.PROCUREMENT_PLAN);
    }

    /**
     * 校验物料数量
     * @param requestVO
     */
    private void checkMaterialsList(ProcurementPlanRequestVO requestVO) {
        // 获取物料
        ContractPlanMaterialListQueryVO queryVO = new ContractPlanMaterialListQueryVO();

        ProcurementPlan procurementPlan = requestVO.getProcurementPlan();
        ContractPlanning contractPlanning = requestVO.getContractPlanning();

        queryVO.setProjectId(contractPlanning.getProjectId());
        queryVO.setConPlanId(contractPlanning.getContractPlanningId());
        queryVO.setProcurementType(procurementPlan.getProcurementPlanType());
        queryVO.setConPlanCode(contractPlanning.getContractPlanningCode());
        List<ContractMaterialsListVO> materialsListList = contractPlanService.getContractMaterialsList(queryVO);

        if (requestVO.getSplitRequestList().size() > 10) {
            throw new ParamValidateException("合约拆分上限不能超过 10 份");
        }

        /*
         * 校验清单是否缺少
         * 获取上限价、物料总数
         */
        Integer procurementPlanType = procurementPlan.getProcurementPlanType();
        Integer priceType = procurementPlan.getPriceType();
        // 上限价
        BigDecimal plannedPrice = BigDecimal.ZERO;
        // 物料总数量
        Map<String,BigDecimal> materialsCountMap = new HashMap<>();

        // 交易标的物
        List<String> subjectMatterNameList = new ArrayList<>();
        List<String> subjectMatterCodeList = new ArrayList<>();
        for (ContractPlanningSplitRequestVO splitRequest : requestVO.getSplitRequestList()) {
            Map<String,MaterialsList> materialsListMap = splitRequest.getMaterialsLists().stream()
                    .collect(Collectors.toMap(MaterialsList::getMaterialsUniqueId, val -> val));
            for(ContractMaterialsListVO listVO : materialsListList) {
                MaterialsList materials = materialsListMap.get(listVO.getMaterialsUniqueId());
                if (materials == null) {
                    throw new BusinessException("提交的合约拆分["+splitRequest.getSplitContractName()+"]的清单中缺少:" + listVO.getMaterialsCode() + "-" + listVO.getMaterialsName());
                }

                // 如果是租赁材料、租赁设备，且租赁方式为日、月，需要重新计算清单数量，即工作量
                if (ProcurementPlanTypeEnum.isRent(procurementPlanType) && !RentModeEnum.isWork(materials.getRentMode())) {
                    materials.setCount(NumberUtil.multiply(materials.getRentTime(),materials.getRentQuantity(),2));
                }

                // 存在业主不输入清单数量的情况，如果不输入，直接默认为 0
                if (materials.getCount() == null) {
                    materials.setCount(BigDecimal.ZERO);
                }

                // 清单数量
                BigDecimal materialsCount = materialsCountMap.get(materials.getMaterialsUniqueId());
                materialsCount = materialsCount == null ? BigDecimal.ZERO : materialsCount;
                materialsCountMap.put(materials.getMaterialsUniqueId(),NumberUtil.add(materialsCount,materials.getCount()));

                // 上限价
                if (PriceTypeEnum.FLOAT_PRICE.equalsType(priceType)) {
                    // 浮动价 = 清单数量 * (基价 + 浮动价 + 卸费)
                    BigDecimal floatPrice = NumberUtil.add(materials.getBasePrice(),materials.getFloatingPrice(),materials.getUnloadingFee());
                    BigDecimal floatPriceAmount = AmountCalUtil.calTotalAmountInclTax(materials.getCount(),floatPrice);
                    plannedPrice = NumberUtil.add(plannedPrice,floatPriceAmount);
                } else {
                    // 价格 = 清单数量 * 含税单价
                    plannedPrice = NumberUtil.add(plannedPrice, AmountCalUtil.calTotalAmountInclTax(materials.getCount(),materials.getUnitPriceInclTax()));
                }

                /*
                 * 处理一些空值
                 */
                // 非租赁，将租赁相关字段全部设置为 null
                if (!ProcurementPlanTypeEnum.isRent(procurementPlanType)) {
                    materials.setRentMode(null);
                    materials.setRentTime(null);
                    materials.setRentQuantity(null);
                }

                // 非浮动价，将浮动价相关字段全部设置为 null
                if (!PriceTypeEnum.FLOAT_PRICE.equalsType(priceType)) {
                    materials.setBasePrice(null);
                    materials.setFloatingPrice(null);
                    materials.setUnloadingFee(null);
                }

                // 租赁且类型为工作量，租赁时间、租赁数量设置为 null
                if (ProcurementPlanTypeEnum.isRent(procurementPlanType) && RentModeEnum.isWork(materials.getRentMode())) {
                    materials.setRentTime(null);
                    materials.setRentQuantity(null);
                }

                // 处理交易标的物
                if (Constants.SUBJECT_MATTER_BLANK.equals( materials.getSubjectMatterCode())) {
                    throw new ParamValidateException(String.format("清单[%s]的交易标的物为空，请确认",materials.getMaterialsName()));
                }
                subjectMatterNameList.add(materials.getSubjectMatterName());
                subjectMatterCodeList.add(materials.getSubjectMatterCode());
            }
        }

        // 交易标的物名称
        String subjectMatterName = subjectMatterNameList.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
        procurementPlan.setSubjectMatterName(subjectMatterName);

        // 交易标的物编码
        String subjectMatterCode = subjectMatterCodeList.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
        procurementPlan.setSubjectMatter(subjectMatterCode);
        // 交易标的物类型，只有购买材料的需要计算
        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(procurementPlan.getProcurementPlanType())) {
            Integer subjectMatterType = materialsListService.getSubjectMatterType(subjectMatterCode);
            procurementPlan.setSubjectMatterType(subjectMatterType);
        } else {
            // 非购买材料 去掉固定价和浮动价相关字段值
            procurementPlan.setPriceType(null);
            procurementPlan.setRegionProvinceCode(null);
            procurementPlan.setRegionCityCode(null);
            procurementPlan.setCountingType(null);
            procurementPlan.setPaymentType(null);
        }

        /*
         * 校验同种清单的租赁方式必须为一致
         */
        if (ProcurementPlanTypeEnum.isRent(procurementPlanType)) {
            Map<String,Set<String>> rentModeMap = requestVO.getSplitRequestList().stream().flatMap(list -> list.getMaterialsLists().stream())
                    .collect(Collectors.groupingBy(
                            MaterialsList::getMaterialsUniqueId,
                            Collectors.mapping(MaterialsList::getRentMode, Collectors.toSet())
                    ));
            for (Map.Entry<String,Set<String>> entry : rentModeMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    throw new BusinessException("清单[" + entry.getKey() + "]的租赁方式存在多种，请重新选择");
                }
            }
        }

        /*
         * 校验上限价
         */
        plannedPrice = NumberUtil.round(plannedPrice,2);
        if (plannedPrice.compareTo(contractPlanning.getPlanningBalance()) > 0) {
            throw new ParamValidateException(String.format("您的合约拆分总价[%s]已超规划余量[%s]无法提交，请重新调整",
                                NumberUtil.decimalFormat(plannedPrice,2),NumberUtil.decimalFormat(contractPlanning.getPlanningBalance(),2)));
        }
        // 校验数量
        for (ContractMaterialsListVO materialsListVO : materialsListList) {
            BigDecimal materialsCount = materialsCountMap.get(materialsListVO.getMaterialsUniqueId());
            if (materialsListVO.getCount().compareTo(materialsCount) < 0) {
                throw new BusinessException("清单[" + materialsListVO.getMaterialsName() + "]，您输入的清单数量[" + materialsCount + "]大于商务策划的剩余量[" + materialsListVO.getCount() + "]，请重新输入");
            }
        }
    }

    /**
     * 拆分数据回写至商务策划
     */
    private void updatePlanQuantityAmount(Long procurementPlanId,String operateFlag) {
        ContractPlanning contractPlanning = contractPlanningService.getByProcurementId(procurementPlanId);
        ValidateUtils.isNullException(contractPlanning,"该采购计划对应的合约规划数据不存在，请确认");

        String contractPlanningId = contractPlanning.getContractPlanningId();
        String projectId = contractPlanning.getProjectId();
        String projectCode = contractPlanning.getProjectCode();

        // 获取采购计划的所有物料
        List<Long> procurementPlan = new ArrayList<>();
        procurementPlan.add(procurementPlanId);
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByPlanIds(procurementPlan);

        contractPlanService.updatePlanQuantityAmount(contractPlanningId,projectId,projectCode,procurementPlanId,contractPlanning.getContractPlanningCategory(),
                            materialsLists,operateFlag);
    }

    private String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return sdf.format(date);
    }

}
