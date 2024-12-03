package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementMaterialsListService;
import com.zhaocai.business.agreement.vo.req.AgreementSchemeQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementSchemeListVO;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.vo.res.BiddingQuotationDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingVendorVO;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.BpmAuditResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.procurement.domain.*;
import com.zhaocai.business.procurement.mapper.ProcurementSchemeMapper;
import com.zhaocai.business.procurement.service.*;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ContractSplitMaterialsQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeRequestVO;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.business.pub.service.IBusinessCodeService;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购方案Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class ProcurementSchemeServiceImpl extends ServiceImpl<ProcurementSchemeMapper,ProcurementScheme> implements IProcurementSchemeService {

    @Autowired
    @Lazy
    private IProcurementPlanService procurementPlanService;

    @Autowired
    private IBusinessCodeService businessCodeService;

    @Autowired
    private IMaterialsListService materialsListService;

    @Autowired
    private IProcurementSchemeBiddingService procurementSchemeBiddingService;

    @Autowired
    private IProcurementSchemePlanRelateService procurementSchemePlanRelateService;

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Autowired
    private ITemplateService templateService;

    @Autowired
    private IMinProjectService minProjectService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private IBPMProcessService processService;

    @Autowired
    private IAgreementMaterialsListService agreementMaterialsListService;

    @Override
    public PageResult<ProcurementSchemeListVO> listPage(ProcurementSchemeListQueryVO queryVO) {
        // ToDo 获取当前登录用户，判断是否为领导，如果是领导则可以看到所有
        queryVO.setIsLeader(0);
        queryVO.setProcurementOfficer(SecurityUtils.getUserId());
        IPage<ProcurementSchemeListVO> iPage = baseMapper.selectPageList(queryVO.toMybatisPage(), queryVO);

        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<BiddingSchemeListVO> biddingSchemeListPage(BiddingSchemeListQueryVO queryVO) {
        queryVO.setState(ProcurementSchemeStateEnum.APPROVE.getState());
        if(null == queryVO.getType()){
            //设置当前登录用户为采购经办人的查询条件
            queryVO.setIsLeader(0);
            queryVO.setProcurementOfficer(SecurityUtils.getUserId());
            queryVO.setFinanceConfirmId(SecurityUtils.getUserId().toString());
        }
        IPage<BiddingSchemeListVO> iPage = baseMapper.selectBiddingSchemePageList(queryVO.toMybatisPage(), queryVO);
        iPage.getRecords().forEach(item -> {
            if (item.getNoticeStatus() != null) {
                item.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(item.getNoticeStatus()));
            }
            // 采购方案招标信息
            ProcurementSchemeBiddingVO procurementSchemeBidding = procurementSchemeBiddingService.getBiddingTemplateBySchemeId(item.getId());
            if (!ObjectUtils.isEmpty(procurementSchemeBidding)) {
                item.setBiddingTemplate(procurementSchemeBidding.getBiddingTemplate());
                item.setBidContactPerson(procurementSchemeBidding.getBidContactPerson());
                item.setBidContactPhone(procurementSchemeBidding.getBidContactPhone());
                item.setBidContactEmail(procurementSchemeBidding.getBidContactEmail());
                item.setBidDeadline(procurementSchemeBidding.getBidDeadline());
            }

            Boolean purchaseOfficerVal = Boolean.FALSE;
            if (null != SecurityUtils.getUserId() && SecurityUtils.getUserId().equals(item.getCreateId())){
                purchaseOfficerVal = Boolean.TRUE;
            }
            item.setPurchaseOfficer(purchaseOfficerVal);
        });
        return new PageResult<>(iPage);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Long saveProcurementScheme(ProcurementSchemeRequestVO requestVO) {
        /* 判断保证金状态 来赋值 */
        setSchemeDeposit(requestVO.getProcurementScheme());

        if (NumberUtil.isNullOrZero(requestVO.getProcurementScheme().getId())) {
            // 校验选择的物料
            ProcurementSchemeCreateVO schemeCreateVO = checkProcurementSchemeData(requestVO.getContractSplitIds());

            addProcurementScheme(requestVO, schemeCreateVO);
        } else {
            // 修改
            updateProcurementScheme(requestVO);
        }

        return requestVO.getProcurementScheme().getId();
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void submitProcurementScheme(Long id,String detailUrl,String operateComment) {
        ProcurementScheme procurementScheme = super.getById(id);
        ValidateUtils.isNullException(procurementScheme, "该采购方案不存在");

        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", procurementScheme.getId());
        paramMap.put("businessTitle", "采购方案审批");
        paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.PROCUREMENT_SCHEME.getDesc(),
                procurementScheme.getProcurementSchemeName()));
        paramMap.put("detailUrl", detailUrl);
        paramMap.put("projectCode", procurementScheme.getProjectCode());
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_PROCUREMENT_SCHEME.name()).
                businessId(id.toString())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));
        paramMap.put("operateComment", operateComment);

        /** 合同类型（contractType），价格(contractMoney)，项目部（parentProjectCode），责任单位（responsibilityDeptId），公司（companyId） */

        paramMap.put("contractType", ProcurementPlanTypeEnum.getProcessType(procurementScheme.getProcurementPlanType()));/* 采购方案 合同类型 */
        paramMap.put("contractMoney", procurementScheme.getCeilingPrice());/* 采购方案上限价 价格 */
        processService.startProcessInstance(
                ProcessKeyEnum.ZHAOCAI_PROCUREMENT_SCHEME.getIdentifying(),paramMap);
    }

    @Override
    public ProcurementSchemeDetailVO detail(Long id) {
        ProcurementScheme procurementScheme = baseMapper.selectById(id);
        ValidateUtils.isNullException(procurementScheme, "该采购方案不存在，请确认");

        // 采购方案
        ProcurementSchemeVO procurementSchemeVO = BeanCopierUtil.copyBean(procurementScheme, ProcurementSchemeVO.class);

        // 采购方案招标信息
        ProcurementSchemeBiddingVO procurementSchemeBidding = procurementSchemeBiddingService.getBySchemeId(id);


        // 采购计划数据
        List<ProcurementSchemePlanRelate> relateList = procurementSchemePlanRelateService.listBySchemeId(id);
        List<Long> contractSplitList = relateList.stream().map(ProcurementSchemePlanRelate::getContractSplitId).collect(Collectors.toList());
        List<ProcurementContractPlanListVO> contractPlanList = contractPlanningService.listProcurementContractPlanByContractSplit(contractSplitList);

        // 获取项目基本信息
        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        procurementSchemeVO.setProjectDeptId(minProjectVO.getDeptId());

        return ProcurementSchemeDetailVO.builder()
                .procurementScheme(procurementSchemeVO)
                .procurementSchemeBidding(procurementSchemeBidding)
                .contractPlanList(contractPlanList)
                .contractSplitIdList(contractSplitList)
                .build();
    }

    @Override
    public List<ProcurementSchemeVO> planSchemeDetail(Long id) {
        /* 获取该采购方案关联关系的采购计划 */
        ProcurementSchemePlanRelate planRelate = procurementSchemePlanRelateService.getOne(new LambdaQueryWrapper<ProcurementSchemePlanRelate>()
                .eq(ProcurementSchemePlanRelate::getProcurementSchemeId,id).last("limit 1"));
        ValidateUtils.isNullException(planRelate, "查询不到该采购方案对应的采购计划数据");
        /* 根据采购计划获取对应的采购方案关联关系 */
        List<ProcurementSchemePlanRelate> planRelateList = procurementSchemePlanRelateService.list(new LambdaQueryWrapper<ProcurementSchemePlanRelate>()
                .eq(ProcurementSchemePlanRelate::getProcurementPlanId,planRelate.getProcurementPlanId()));
        ValidateUtils.isNullException(planRelateList, "属于该采购计划的采购方案列表数据查询不到");
        /* 提取采购方案ids */
        List<Long> schemeIds = planRelateList.stream().map(ProcurementSchemePlanRelate::getProcurementSchemeId).collect(Collectors.toList());
        /* 获取采购方案列表 */
        List<ProcurementScheme> procurementSchemes = list(new LambdaQueryWrapper<ProcurementScheme>()
                .in(ProcurementScheme::getId, schemeIds).ne(ProcurementScheme::getState,ProcurementSchemeStateEnum.CANCELLATION.getState()));
        if(procurementSchemes==null || procurementSchemes.isEmpty())
            return Collections.emptyList();
        /* 格式化采购方案返回对象 */
        return procurementSchemes.stream()
                .map(info -> BeanCopierUtil.copyBean(info, ProcurementSchemeVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MaterialsVO> listMaterials(Long id) {
        List<ProcurementSchemePlanRelate> relateList = procurementSchemePlanRelateService.listBySchemeId(id);
        List<Long> planIdList = relateList.stream()
                .map(ProcurementSchemePlanRelate::getProcurementPlanId)
                .collect(Collectors.toList());

        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByPlanIds(planIdList);
        //将相同的物料进行合并
        Map<String, MaterialsList> materialsListMap = new HashMap<>();
        MaterialsList materials;
        for (MaterialsList materialsList : materialsLists) {
            materials = materialsListMap.get(materialsList.getMaterialsCode());
            if (materials != null) {
                materials.setCount(materials.getCount().add(materialsList.getCount()));
                materialsListMap.put(materialsList.getMaterialsCode(), materials);
            } else {
                materialsListMap.put(materialsList.getMaterialsCode(), materialsList);
            }
        }
        List<MaterialsVO> materialsVOList = BeanCopierUtil.copyList(new ArrayList<>(materialsListMap.values()), MaterialsVO.class);

        /* 排序一下 根据 物料编码 */
        materialsVOList.stream().sorted(Comparator.comparing(MaterialsVO::getMaterialsCode).reversed()).collect(Collectors.toList());

        return materialsVOList;
    }

    @Override
    public List<CompMaterialsVO> listCompMaterials(Long id) {
        List<CompMaterialsVO> dataVo = new ArrayList<>();
        // 通过采购方案id查询 采购方案-采购计划对应关系
        List<ProcurementSchemePlanRelate> relateList = procurementSchemePlanRelateService.listBySchemeId(id);
        //获取 拆分合约id
        List<Long> contractSplitList = relateList.stream().map(ProcurementSchemePlanRelate::getContractSplitId).collect(Collectors.toList());
        //获取 采购计划id
        List<Long> procurementPlanList = relateList.stream().map(ProcurementSchemePlanRelate::getProcurementPlanId).collect(Collectors.toList());
        ProcurementPlan plan = procurementPlanService.getById(procurementPlanList.get(0));
        //获取合约规划信息
        List<ProcurementContractPlanListVO> contractPlanList = contractPlanningService.listProcurementContractPlanByContractSplit(contractSplitList);
        contractPlanList.forEach(item -> {
            CompMaterialsVO compMaterialsVO = new CompMaterialsVO();
            compMaterialsVO.setPlanId(item.getPlanId());
            compMaterialsVO.setPriceType(plan.getPriceType());
            compMaterialsVO.setContractPlanningName(item.getContractPlanningName());

            List<CompContractSplitMaterialsVO> compVOList = new ArrayList<>();
            ContractSplitMaterialsQueryVO queryVO = new ContractSplitMaterialsQueryVO();
            queryVO.setPlanId(item.getPlanId());
            queryVO.setContractSpiltIdList(contractSplitList);
            List<CompContractSplitMaterialsVO> contractSplitMaterials = materialsListService.listContractSplitMaterials4Bidding(queryVO);
            contractSplitMaterials.forEach(compVO -> {
                    compVO.setCompName("（" + item.getContractPlanningName() + "）" + compVO.getSplitContractName());
                    compVO.setPriceType(plan.getPriceType());
                    compVOList.add(compVO);
            });

            compMaterialsVO.setCompVOList(compVOList);
            dataVo.add(compMaterialsVO);
        });
        return dataVo;
    }

    @Override
    public List<ProcurementPlanListVO> listProcurementPlanByScheme(Long id) {
        return baseMapper.selectProcurementPlanListByScheme(id);
    }

    @Override
    public ProcurementSchemeCreateVO getProcurementSchemeCreateInfo(List<Long> contractSplitIds) {
        ProcurementSchemeCreateVO schemeCreate = this.checkProcurementSchemeData(contractSplitIds);

        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(schemeCreate.getProjectCode());
        schemeCreate.setProjectDeptId(minProjectVO.getDeptId());
        return schemeCreate;
    }

    @Override
    public PageResult<AgreementSchemeListVO> listSignAgreementScheme(AgreementSchemeQueryVO queryVO) {
        IPage<AgreementSchemeListVO> pages = baseMapper.selectSignAgreementSchemeList(queryVO.toMybatisPage(), queryVO);
        return new PageResult<>(pages);
    }

    @Override
    public AttachmentVO getAgreementTemplateAttachmentInfo(Long id) {
        ProcurementSchemeBidding schemeBidding = procurementSchemeBiddingService.getDomainBySchemeId(id);
        ValidateUtils.isNullException(schemeBidding, "该采购方案的招标信息不存在，请确认");

        return templateService.getTemplateAttachmentInfo(schemeBidding.getContractTemplateId());
    }

    @Override
    public List<MinProjectDataVO> selectDataByScheme(Long id) {
        return baseMapper.selectDataByScheme(id);
    }

    @Override
    public void cancellationProcurementScheme(Long id) {
        ProcurementScheme procurementScheme = super.getById(id);
        ValidateUtils.isNullException(procurementScheme,"该采购方案不存在");

        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.CANCELLATION.getState())
                .eq(ProcurementScheme::getId,id));
    }

    @Override
    public void cancellationProcurementSchemePlan(Long id) {
        ProcurementScheme procurementScheme = super.getById(id);
        ValidateUtils.isNullException(procurementScheme,"该采购方案不存在");

        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.CANCELLATION.getState())
                .eq(ProcurementScheme::getId,id));
        /* 获取第一条采购方案对应的采购计划 */
        ProcurementSchemePlanRelate procurementSchemePlanRelate = procurementSchemePlanRelateService.getOne(new LambdaQueryWrapper<ProcurementSchemePlanRelate>()
                .eq(ProcurementSchemePlanRelate::getProcurementSchemeId,procurementScheme.getId()).last("limit 1"));
        ValidateUtils.isNullException(procurementSchemePlanRelate,"查询不到该采购方案对应的采购计划。");
        /* 废除采购计划，如果存在除当前被废除的采购方案外的采购方案没有被废除就无法废除该采购计划。 */
        procurementPlanService.cancellationProcurementPlan(procurementSchemePlanRelate.getProcurementPlanId());
    }

    @Override
    public void revokeProcurementScheme(Long id) {
        ProcurementScheme procurementScheme = this.getById(id);
        ValidateUtils.validateStatusNotEquals(ProcurementSchemeStateEnum.IN_APPROVAL::equalsState, procurementScheme.getState(), "非审批中的采购方案不允许撤回");
        // 撤回流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", procurementScheme.getId());
        paramMap.put("processId", procurementScheme.getWfProcessId());
        processService.revokeProcess(ProcessKeyEnum.ZHAOCAI_PROCUREMENT_SCHEME.getIdentifying(),paramMap);
    }

    @Override
    public void processAuditRevoke(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.REVOKED.getState())
                .eq(ProcurementScheme::getId, businessId));
    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        ProcurementScheme procurementScheme = this.getById(requestDTO.getBusinessId());
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        /* 最小核算项目 */
        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        if (null != minProjectVO) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId()));/* 公司 二级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", minProjectVO.getDutyUnit());/* 责任单位 三级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", minProjectVO.getParentCode());/* 父项目编码(项目部) */
            requestDTO.setPropertyList(propertyList);
        }
        requestDTO.setPropertyList(propertyList);
        return processService.initialize(requestDTO);
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        ProcurementScheme procurementScheme = this.getById(requestDTO.getBusinessId());
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        /* 最小核算项目 */
        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        if (null != minProjectVO) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId()));/* 公司 二级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", minProjectVO.getDutyUnit());/* 责任单位 三级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", minProjectVO.getParentCode());/* 父项目编码(项目部) */
            requestDTO.setPropertyList(propertyList);
        }
        requestDTO.setPropertyList(propertyList);
        return processService.listProcessLog(requestDTO);
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        ProcurementScheme procurementScheme = getById((Serializable) variables.get("businessId"));
        /* 最小核算项目 */
        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        if (null != minProjectVO) {
            /* 流程角色配置规则传参 */
            variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            variables.put("companyId", underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId()));/* 公司 二级单位 */
            variables.put("responsibilityDeptId", minProjectVO.getDutyUnit());/* 责任单位 三级单位 */
            variables.put("parentProjectCode", minProjectVO.getParentCode());/* 父项目编码(项目部) */
        }
        return processService.auditProcessInstance(ProcessKeyEnum.ZHAOCAI_PROCUREMENT_SCHEME.getIdentifying(),variables);
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        ProcurementScheme procurementScheme = this.getById(requestDTO.getBusinessId());
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        /* 最小核算项目 */
        MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        if (null != minProjectVO) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId()));/* 公司 二级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", minProjectVO.getDutyUnit());/* 责任单位 三级单位 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", minProjectVO.getParentCode());/* 父项目编码(项目部) */
            requestDTO.setPropertyList(propertyList);
        }
        requestDTO.setPropertyList(propertyList);
        return processService.loadTaskDef(requestDTO);
    }
    /**
     * 发起审批
     * @param variables
     */
    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer procurementSchemeState = ProcurementSchemeStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            procurementSchemeState = ProcurementSchemeStateEnum.APPROVE.getState();
        }
        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getWfProcessId, processId)
                .set(ProcurementScheme::getState, procurementSchemeState)
                .eq(ProcurementScheme::getId, businessId));
    }


    /**
     * 审批通过
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.APPROVE.getState())
                .eq(ProcurementScheme::getId, businessId));
    }

    /**
     * 审批驳回到发起人
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.DRAFT.getState())
                .eq(ProcurementScheme::getId, businessId));
    }

    /**
     * 审批驳回
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<ProcurementScheme>()
                .set(ProcurementScheme::getState, ProcurementSchemeStateEnum.REJECT.getState())
                .eq(ProcurementScheme::getId, businessId));
    }

    /**
     * 校验数据
     *
     * @param contractSplitIds
     */
    private ProcurementSchemeCreateVO checkProcurementSchemeData(List<Long> contractSplitIds) {
        ProcurementSchemeCreateVO schemeCreateVO = new ProcurementSchemeCreateVO();

        // 获取合约拆分记录
        List<ContractPlanningSplit> planningSplits = contractPlanningSplitService.listByIds(contractSplitIds);

        // 获取&校验采购计划
        List<Long> planIdList = extractDistinctValues(planningSplits,ContractPlanningSplit::getProcurementPlanId);
        List<ProcurementPlan> procurementPlans = procurementPlanService.listByIds(planIdList);

        // 校验采购类型
        List<Integer> procurementPlanType = extractDistinctValues(procurementPlans,ProcurementPlan::getProcurementPlanType);
        if (procurementPlanType.size() > 1) {
            throw new ParamValidateException("所选择的采购计划存在多种采购类型，请确认后重新选择");
        }
        schemeCreateVO.setProcurementPlanType(procurementPlanType.get(0));
        schemeCreateVO.setProcurementOfficerName(procurementPlans.get(0).getProcurementOfficerName());
        schemeCreateVO.setFirstProcurementPlanId(planIdList.get(0));
        schemeCreateVO.setProcurementSchemeName(procurementPlans.get(0).getProcurementPlanName());

        /*
         * 购买材料需要做一些校验
         */
        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(procurementPlanType.get(0))) {
            List<Integer> subjectMatterType = procurementPlans.stream()
                    .filter(Objects::nonNull)
                    .map(ProcurementPlan::getSubjectMatterType)
                    .distinct()
                    .collect(Collectors.toList());
            if (subjectMatterType.size() > 1) {
                throw new ParamValidateException("所选择的采购计划存在多种不同交易标的物类型，请确认后重新选择");
            }
            schemeCreateVO.setSubjectMatterType(subjectMatterType.get(0));

            /* 获取采购计划的价格类型列表 校验价格类型 */
            List<Integer> priceTypes = extractDistinctValues(procurementPlans,ProcurementPlan::getPriceType);
            if (priceTypes.size() > 1) {
                throw new ParamValidateException("所选择的采购计划存在多种价格类型，请确认后重新选择");
            }
            schemeCreateVO.setPriceType(priceTypes.get(0));

            // 计数方式
            List<Integer> countingTypes = extractDistinctValues(procurementPlans,ProcurementPlan::getCountingType);
            if (countingTypes.size() > 1) {
                throw new ParamValidateException("所选择的采购计划存在多种计数方式，请确认后重新选择");
            }
            schemeCreateVO.setCountingType(countingTypes.get(0));

            // 付款方式
            List<Integer> paymentTypes = extractDistinctValues(procurementPlans,ProcurementPlan::getPaymentType);
            if (paymentTypes.size() > 1) {
                throw new ParamValidateException("所选择的采购计划存在多种付款方式，请确认后重新选择");
            }
            schemeCreateVO.setPaymentType(paymentTypes.get(0));
        }

        // 校验合约拆分是否已被其他方案使用
        List<ContractPlanningSplit> existPlanningSplits = contractPlanningSplitService.listExistPlanningSplits(contractSplitIds);
        if (CollectionUtil.isNotEmpty(existPlanningSplits)) {
            String splitName = existPlanningSplits.stream()
                    .map(ContractPlanningSplit::getSplitContractName)
                    .collect(Collectors.joining(","));
            throw new BusinessException("您所选择的合约拆分:" + splitName + "已被其他采购方案使用，请重新选择");
        }

        // 判断是否为同一个项目
        List<ProcurementContractPlanListVO> contractPlanningList = contractPlanningService.listByPlanIds(planIdList);
        List<String> projectCodesList = extractDistinctValues(contractPlanningList,ProcurementContractPlanListVO::getProjectCode);
        if (projectCodesList.size() > 1) {
            throw new ParamValidateException("所现在的采购计划归属项目，请确认后重新选择");
        }
        schemeCreateVO.setProjectCode(projectCodesList.get(0));

        // 校验经办人
        List<Long> procurementOfficerList = extractDistinctValues(procurementPlans,ProcurementPlan::getProcurementOfficer);
        if (procurementOfficerList.size() > 1) {
            throw new ParamValidateException("所选择的采购计划存在多个采购经办人，请确认后重新选择");
        }
        if (!procurementOfficerList.get(0).equals(SecurityUtils.getUserId())) {
            throw new ParamValidateException("所选择的采购计划的采购经办人不是您本人");
        }

        //计算上限价和交易标的物
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(contractSplitIds);
        BigDecimal ceilingPrice = BigDecimal.ZERO;
        List<String> subjectMatterCodeSet = new ArrayList<>();
        List<String> subjectMatterNameSet = new ArrayList<>();

        for (MaterialsList materials : materialsLists) {
            ceilingPrice = ceilingPrice.add(materials.getAmountInclTax());

            subjectMatterCodeSet.add(materials.getSubjectMatterCode());
            subjectMatterNameSet.add(materials.getSubjectMatterName());
        }

        schemeCreateVO.setCeilingPrice(NumberUtil.round(ceilingPrice, Constants.SCALE_AMOUNT_VO));
        schemeCreateVO.setSubjectMatterName(subjectMatterNameSet.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining(",")));
        schemeCreateVO.setSubjectMatterCode(subjectMatterCodeSet.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining(",")));

        return schemeCreateVO;
    }

    private <T, R> List<R> extractDistinctValues(List<T> list, Function<T, R> mapper) {
        return list.stream()
                .map(mapper)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 新增采购方案
     *
     * @param requestVO
     */
    private void addProcurementScheme(ProcurementSchemeRequestVO requestVO, ProcurementSchemeCreateVO schemeCreateVO) {
        // 基本信息
        ProcurementScheme procurementScheme = requestVO.getProcurementScheme();
        procurementScheme.setProcurementSchemeCode(getProcurementSchemeCode());
        procurementScheme.setProcurementOfficer(SecurityUtils.getUserId());
        procurementScheme.setProcurementOfficerName(SecurityUtils.getLoginUser().getSysUser().getNickName());
        procurementScheme.setProcurementPlanType(schemeCreateVO.getProcurementPlanType());
        procurementScheme.setCeilingPrice(schemeCreateVO.getCeilingPrice());
        procurementScheme.setProjectCode(schemeCreateVO.getProjectCode());
        procurementScheme.setCountingType(schemeCreateVO.getCountingType());
        procurementScheme.setPaymentType(schemeCreateVO.getPaymentType());
        procurementScheme.setPriceType(schemeCreateVO.getPriceType());
        procurementScheme.setState(ProcurementSchemeStateEnum.DRAFT.getState());
        procurementScheme.setSubjectMatterType(schemeCreateVO.getSubjectMatterType());
        procurementScheme.setSubjectMatterCode(schemeCreateVO.getSubjectMatterCode());
        procurementScheme.setSubjectMatterName(schemeCreateVO.getSubjectMatterName());

        baseMapper.insert(procurementScheme);

        // 招标文件
        procurementSchemeBiddingService.saveProcurementSchemeBidding(requestVO.getProcurementSchemeBidding(), procurementScheme.getId());

        // 关联关系
        procurementSchemePlanRelateService.saveRelate(requestVO.getContractSplitIds(), procurementScheme.getId());
    }

    /**
     * 获取采购方案编码
     *
     * @return
     */
    private String getProcurementSchemeCode() {
        return "CGRW" + LocalDate.now().getYear() + businessCodeService.getBusinessCode(BusinessCodeEnum.PROCUREMENT_SCHEME);
    }

    /**
     * 修改采购方案
     *
     * @param requestVO
     */
    private void updateProcurementScheme(ProcurementSchemeRequestVO requestVO) {
        // 基本信息
        ProcurementScheme procurementScheme = requestVO.getProcurementScheme();
        ProcurementScheme checkScheme = baseMapper.selectById(procurementScheme.getId());

        ValidateUtils.isNullException(checkScheme, "该采购方案不存在");
        if (!checkScheme.getCreateId().equals(SecurityUtils.getUserId())) {
            throw new ParamValidateException("你无权编辑不属于您的采购方案");
        }

        baseMapper.updateById(procurementScheme);

        // 招标文件模板附件 合同模板附件
        procurementSchemeBiddingService.updateProcurementSchemeBidding(requestVO.getProcurementSchemeBidding(), procurementScheme.getId());
    }

    /**
     * 设置保证金
     * @param procurementScheme
     */
    private void setSchemeDeposit(ProcurementScheme procurementScheme) {
        if (procurementScheme != null && procurementScheme.getIsReceiveDeposit() != 1) {
            procurementScheme.setSecurityDeposit(null);
            procurementScheme.setFinanceConfirmId(null);
            procurementScheme.setFinanceConfirmName(null);
        }
    }

}
