package com.zhaocai.business.expert.service.impl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.domain.ExpertChange;
import com.zhaocai.business.expert.mapper.ExpertMapper;
import com.zhaocai.business.expert.service.IExpertChangeService;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.expert.vo.req.ExpertVO;
import com.zhaocai.business.expert.vo.req.query.ExpertQueryVO;
import com.zhaocai.business.expert.vo.req.query.ExpertRandomDrawVO;
import com.zhaocai.business.expert.vo.res.ExpertChangeInfoVO;
import com.zhaocai.business.expert.vo.res.ExpertInfoVO;
import com.zhaocai.business.expert.vo.res.ExpertListVO;
import com.zhaocai.business.expert.vo.res.TPIExpertInfoVO;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISystemUserService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 专家Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Slf4j
@Service
public class ExpertServiceImpl extends ServiceImpl<ExpertMapper,Expert> implements IExpertService {

    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private IAttachmentService attachmentService;
    @Autowired
    private IBPMProcessService processService;
    @Autowired
    private UnderlingSystemService underlingSystemService;
    @Autowired
    private ISystemUserService systemUserService;
    @Autowired
    private IExpertChangeService expertChangeService;

    @Override
    public List<TPIExpertInfoVO> getTPIExpertInfo() {
        List<TPIExpertInfoVO> list = new ArrayList<>();
        TPIExpertInfoVO vo1 = new TPIExpertInfoVO();
        vo1.setExpertId(461234648744L);
        vo1.setExpertName("张明");
        vo1.setExpertPhone("13565485522");
        vo1.setOrganizationId(97413131L);
        vo1.setBelongOrganization("直属三公司");
        vo1.setDepartmentId(1356548552L);
        vo1.setDepartment("商务部");
        list.add(vo1);

        TPIExpertInfoVO vo2 = new TPIExpertInfoVO();
        vo2.setExpertId(45678624136L);
        vo2.setExpertName("李霞");
        vo2.setExpertPhone("13651256325");
        vo2.setOrganizationId(7845236456L);
        vo2.setBelongOrganization("直属三公司");
        vo2.setDepartmentId(76574342L);
        vo2.setDepartment("工程建设部");
        list.add(vo2);

        TPIExpertInfoVO vo4 = new TPIExpertInfoVO();
        vo4.setExpertId(6756345325L);
        vo4.setExpertName("赵特");
        vo4.setExpertPhone("19155256663");
        vo4.setOrganizationId(534653242234L);
        vo4.setBelongOrganization("直属三公司");
        vo4.setDepartmentId(49785464L);
        vo4.setDepartment("技术部");
        list.add(vo4);

        TPIExpertInfoVO vo5 = new TPIExpertInfoVO();
        vo5.setExpertId(634563453L);
        vo5.setExpertName("钱经");
        vo5.setExpertPhone("15112547854");
        vo5.setOrganizationId(786542344234L);
        vo5.setBelongOrganization("直属三公司");
        vo5.setDepartmentId(1231245464575L);
        vo5.setDepartment("工程建设部");
        list.add(vo5);

        return list;
    }

    @Override
    public PageResult<ExpertListVO> pageAll(ExpertQueryVO queryDTO) {
        return page(queryDTO);
    }

    @Override
    public PageResult<ExpertListVO> page(ExpertQueryVO queryDTO) {
        queryDTO.setWorkYearCompareDate(DateUtils.getNowDate());
        if (!CollectionUtils.isEmpty(queryDTO.getNotIncludeExpertIdList())){
            queryDTO.setNotIncludeExpertIds(
                    queryDTO.getNotIncludeExpertIdList().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.EVALUATION_BID.getState());
        if (StringUtils.isNotEmpty(queryDTO.getDeptIds())){
            List<Long> deptIdList = new ArrayList<>();
            String[] deptArr = queryDTO.getDeptIds().split(",");
            for (String deptIdStr : deptArr) {
                deptIdList.add(Long.valueOf(deptIdStr));
            }
            queryDTO.setDeptIdList(deptIdList);
        }
        IPage<ExpertListVO> iPage = new Page<>();
        if (ObjectUtils.isEmpty(queryDTO.getDrawVO())){
            iPage = baseMapper.page(queryDTO.toMybatisPage(), queryDTO);
        } else {
            //查询所有符合条件的数据
            List<ExpertListVO> expertList = baseMapper.listExpert(queryDTO);

            //如果抽取条件不等于空
            if (!ObjectUtils.isEmpty(queryDTO.getDrawVO())){
                expertList = new ArrayList<>(randomDraw(expertList, queryDTO.getDrawVO()));
            }

            //封装分页数据(根据page的分页参数计算当前分页的数据)
            List<ExpertListVO> subList = expertList.stream()
                    .skip((long) (queryDTO.getPageNumber() - 1) * queryDTO.getPageSize())
                    .limit(queryDTO.getPageSize())
                    .collect(Collectors.toList());
            iPage.setRecords(subList);
            iPage.setTotal(expertList.size());
            iPage.setPages(expertList.size() / queryDTO.getPageSize() + 1);
        }
        return new PageResult<>(iPage);
    }

    /**
     * 根据条件随机抽取专家
     * */
    private List<ExpertListVO> randomDraw(List<ExpertListVO> expertList, ExpertRandomDrawVO drawVO){
        List<ExpertListVO> drawExpertList = new ArrayList<>();
        // 使用洗牌算法打乱列表中的元素
        Collections.shuffle(expertList);
        List<ExpertListVO> techExpertList = expertList.stream().filter(item -> item.getExpertType() == 1).collect(Collectors.toList());
        List<ExpertListVO> econExpertList = expertList.stream().filter(item -> item.getExpertType() == 2).collect(Collectors.toList());
        Integer econExpertNum = drawVO.getEconExpertNum();
        Integer techExpertNum = drawVO.getTechExpertNum();

        //获取抽取人数
        for (int i = 0; i < Math.min(techExpertList.size(), techExpertNum); i++) {
            drawExpertList.add(techExpertList.get(i));
        }
        for (int j = 0; j < Math.min(econExpertList.size(), econExpertNum); j++) {
            drawExpertList.add(econExpertList.get(j));
        }
        return drawExpertList;
    }

    @Override
    public ExpertInfoVO getInfo(Long id) {
        Expert expert = this.getById(id);
        ExpertInfoVO vo = BeanCopierUtil.copyBean(expert, ExpertInfoVO.class);
        /* 如果不是审批通过状态 */
        if(!expert.getState().equals(ExpertStateEnum.APPROVE.getState())){
            ExpertChange expertChange = expertChangeService.getOne(new LambdaQueryWrapper<ExpertChange>()
                    .eq(ExpertChange::getExpertId,id).orderByDesc(ExpertChange::getCreateTime).last("limit 1"));
            if(expertChange!=null){
                if(expert.getProcessType().equals(ExpertProcessTypeEnum.EXPERT_CHANGE.getState())){
                    vo = BeanCopierUtil.copyBean(expertChange, ExpertInfoVO.class);
                    vo.setExpertId(id);
                }
            }
        }
        List<AttachmentVO> resumeAttachList = attachmentService.listAttachment(AttachmentTypeEnum.EXPERT_RESUME, vo.getId());
        vo.setResumeAttachList(resumeAttachList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ExpertVO expertVO) {
        long count = 0;
        /* getInfo拿到的是expertChange对象的id. */
        if(expertVO.getExpertId()!=null)
            expertVO.setId(expertVO.getExpertId());
        /* 新增才走审批流程，修改不走 */
        Boolean idIsNull = expertVO.getId()==null;
        if(expertVO.getId()!=null){
            count = this.count(new LambdaQueryWrapper<Expert>()
                            .eq(Expert::getUserId, expertVO.getUserId())
                            .ne(Expert::getId, expertVO.getId()));
        }else {
            count = this.count(new LambdaQueryWrapper<Expert>()
                    .eq(Expert::getUserId, expertVO.getUserId()));
        }
        //如果该用户已经是某个专家了
        if (count > 0) {
            throw new ParamValidateException("该用户已经成为专家，不允许重复设置");
        }

        //新增专家信息
        Expert expert;
        /* 新增才走审批流程，修改不走 */
        if(idIsNull){
            expert = BeanCopierUtil.copyBean(expertVO, Expert.class);
            /* 待审批 */
            expert.setExpertState(NumberConstant.ZERO);
            /* 保存 */
            expert.setState(ExpertStateEnum.SAVE.getState());
            /* 审批类型 */
            expert.setProcessType(ExpertProcessTypeEnum.EXPERT_ADD.getState());
        }else {
            Expert e = getById(expertVO.getId());
            if(e!=null){
                if(e.getState()!=null && e.getState().equals(ExpertStateEnum.IN_APPROVAL.getState())){
                    throw new ParamValidateException("专家正在审批中，请稍后再修改");
                }
                /* 审批通过后的使用新增专家修改对象 */
                if(e.getState()!=null && e.getState().equals(ExpertStateEnum.APPROVE.getState())){
                    ExpertChange expertChange = BeanCopierUtil.copyBean(expertVO, ExpertChange.class);
                    /* 待审批 */
                    expertChange.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expertChange.setState(ExpertStateEnum.SAVE.getState());
                    /* 审批类型 */
                    expertChange.setProcessType(ExpertProcessTypeEnum.EXPERT_CHANGE.getState());
                    expertChange.setId(null);
                    expertChange.setExpertId(expertVO.getId());
                    expertChange.setCreateTime(new Date());
                    expertChange.setCreateId(SecurityUtils.getUserId());
                    expertChange.setCreateBy(SecurityUtils.getLoginUserNickName());
                    expertChangeService.save(expertChange);
                    //保存招标文件附件
                    attachmentService.addAttachment(expertVO.getResumeAttachList(), AttachmentTypeEnum.EXPERT_RESUME, expertChange.getId());

                    /* 审批通过后的修改数据库的值 */
                    expert = e;
                    /* 待审批 */
                    expert.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expert.setState(ExpertStateEnum.SAVE.getState());
                    /* 审批类型 */
                    expert.setProcessType(ExpertProcessTypeEnum.EXPERT_CHANGE.getState());
                }else{
                    /* 审批不通过后的修改数据库的值，类型用原来的。 */
                    expert = e;
                    /* 待审批 */
                    expert.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expert.setState(ExpertStateEnum.SAVE.getState());
                    /* 审批类型，用原来的 */
                    expert.setProcessType(e.getProcessType());
                }
            }else{
                expert = BeanCopierUtil.copyBean(expertVO, Expert.class);
                /* 待审批 */
                expert.setExpertState(NumberConstant.ZERO);
                /* 保存 */
                expert.setState(ExpertStateEnum.SAVE.getState());
                /* 审批类型 */
                expert.setProcessType(ExpertProcessTypeEnum.EXPERT_ADD.getState());
            }
        }

        boolean res = this.saveOrUpdate(expert);

        //保存招标文件附件
        if(expert.getProcessType().equals(ExpertProcessTypeEnum.EXPERT_ADD.getState())){
            attachmentService.addAttachment(expertVO.getResumeAttachList(), AttachmentTypeEnum.EXPERT_RESUME, expert.getId());
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(ExpertVO expertVO) {
        long count = 0;
        /* getInfo拿到的是expertChange对象的id. */
        if(expertVO.getExpertId()!=null)
            expertVO.setId(expertVO.getExpertId());
        /* 新增才走审批流程，修改不走 */
        Boolean idIsNull = expertVO.getId()==null;
        if(expertVO.getId()!=null){
            count = this.count(new LambdaQueryWrapper<Expert>()
                    .eq(Expert::getUserId, expertVO.getUserId())
                    .ne(Expert::getId, expertVO.getId()));
        }else {
            count = this.count(new LambdaQueryWrapper<Expert>()
                    .eq(Expert::getUserId, expertVO.getUserId()));
        }
        //如果该用户已经是某个专家了
        if (count > 0){
            throw new ParamValidateException("该用户已经成为专家，不允许重复设置");
        }


        //新增专家信息
        Expert expert;
        ExpertChange expertChange = null;
        /* 新增才走审批流程，修改不走 */
        if(idIsNull){
            expert = BeanCopierUtil.copyBean(expertVO, Expert.class);
            /* 待审批 */
            expert.setExpertState(NumberConstant.ZERO);
            /* 保存 */
            expert.setState(ExpertStateEnum.IN_APPROVAL.getState());
            /* 审批类型 */
            expert.setProcessType(ExpertProcessTypeEnum.EXPERT_ADD.getState());
        }else {
            Expert e = getById(expertVO.getId());
            if(e!=null){
                if(e.getState()!=null && e.getState().equals(ExpertStateEnum.IN_APPROVAL.getState())){
                    throw new ParamValidateException("专家正在审批中，请稍后再修改");
                }
                /* 审批通过后的使用新增专家修改对象 */
                if(e.getState()!=null && e.getState().equals(ExpertStateEnum.APPROVE.getState())){
                    expertChange = BeanCopierUtil.copyBean(expertVO, ExpertChange.class);
                    /* 待审批 */
                    expertChange.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expertChange.setState(ExpertStateEnum.IN_APPROVAL.getState());
                    /* 审批类型 */
                    expertChange.setProcessType(ExpertProcessTypeEnum.EXPERT_CHANGE.getState());
                    expertChange.setId(null);
                    expertChange.setExpertId(expertVO.getId());
                    expertChange.setCreateTime(new Date());
                    expertChange.setCreateId(SecurityUtils.getUserId());
                    expertChange.setCreateBy(SecurityUtils.getLoginUserNickName());
                    expertChangeService.save(expertChange);
                    //保存招标文件附件
                    attachmentService.addAttachment(expertVO.getResumeAttachList(), AttachmentTypeEnum.EXPERT_RESUME, expertChange.getId());

                    /* 审批通过后的修改数据库的值 */
                    expert = e;
                    /* 待审批 */
                    expert.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expert.setState(ExpertStateEnum.IN_APPROVAL.getState());
                    /* 审批类型 */
                    expert.setProcessType(ExpertProcessTypeEnum.EXPERT_CHANGE.getState());
                }else{
                    /* 审批不通过后的修改数据库的值，类型用原来的。 */
                    expert = e;
                    /* 待审批 */
                    expert.setExpertState(NumberConstant.ZERO);
                    /* 保存 */
                    expert.setState(ExpertStateEnum.IN_APPROVAL.getState());
                    /* 审批类型，用原来的 */
                    expert.setProcessType(e.getProcessType());
                }

            }else{
                expert = BeanCopierUtil.copyBean(expertVO, Expert.class);
                /* 待审批 */
                expert.setExpertState(NumberConstant.ZERO);
                /* 保存 */
                expert.setState(ExpertStateEnum.IN_APPROVAL.getState());
                /* 审批类型 */
                expert.setProcessType(ExpertProcessTypeEnum.EXPERT_ADD.getState());
            }
        }

        boolean res = this.saveOrUpdate(expert);

        //保存招标文件附件
        if(expert.getProcessType().equals(ExpertProcessTypeEnum.EXPERT_ADD.getState())){
            attachmentService.addAttachment(expertVO.getResumeAttachList(), AttachmentTypeEnum.EXPERT_RESUME, expert.getId());
        }

        expert = getById(expert.getId());
        System.out.println("[新增专家审批]"+expert);

        if (res){
            //提交审批信息
            //接入底层逻辑平台流程
            Map<String,Object> paramMap = new HashMap<>();
            if(expert.getProcessType().equals(ExpertProcessTypeEnum.EXPERT_ADD.getState())){
                paramMap.put("businessId", expert.getId());
                paramMap.put("businessTitle", "新增专家审批");
                paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.EXPERT_ADD_APPROVE.getDesc(), expert.getExpertName()));
            }else{
                paramMap.put("businessId", expertChange==null?null:expertChange.getId());
                paramMap.put("businessTitle", "专家信息修改审批");
                paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.EXPERT_CHANGE_APPROVE.getDesc(), expert.getExpertName()));
            }

            SysUser sysUser = systemUserService.getUserById(expert.getUserId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying().replace("{org}",org);
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
            log.info("[获取三级单位]{}",orgThree);
            /* 获取所有流程 */
            List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
            log.info("[获取所有流程]{}",listCataLogDTOS);
            if (listCataLogDTOS != null) {
                /* 判断二级单位流程是否存在 */
                ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
                log.info("[判断二级单位流程是否存在]{}",cataLogDTOTwo);
                if (cataLogDTOTwo != null) {
                    /* 赋值使用二级单位 */
                    customProcessKey = ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying().replace("{org}",org);
                }
                if (orgThree != null) {
                    /* 判断三级单位流程是否存在 */
                    String finalOrgThree = orgThree;
                    ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
                    log.info("[判断三级单位流程是否存在]{}",cataLogDTOThree);
                    if (cataLogDTOThree != null) {
                        /* 赋值使用三级单位 */
                        customProcessKey = ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying().replace("{org}",orgThree);
                    }
                }
            }

            paramMap.put("customProcessKey", customProcessKey);
            paramMap.put("detailUrl", "/expert/expert-detail/"+ Base64.encodeStr(("\""+expert.getId().toString()+"\"").getBytes(),true,true));
            UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.name()).businessId(expert.getId().toString()).toDoType(ToDoTypeEnum.EXAMINE.name()).build();
            paramMap.put("userObj", JSON.toJSONString(userObj));

            /* 获取三级单位 */
            if(orgThree==null)orgThree = org;
            /* 流程角色配置规则传参 */
            paramMap.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            paramMap.put("companyId", org);/* 公司 二级单位 */
            paramMap.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            paramMap.put("parentProjectCode", org);/* 父项目编码(项目部) */

            processService.startProcessInstance(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying(),paramMap);
        }

        /*//创建专家账号
        BusinessUser businessUser = new BusinessUser();
        businessUser.setUserName(expert.getExpertPhone());
        businessUser.setNickName(expert.getExpertName());
        businessUser.setUserType(UserTypeEnum.EXPERT);
        R<Long> r = remoteUserService.addBusinessUser(businessUser, SecurityConstants.INNER);
        if (R.SUCCESS != r.getCode()) {
            throw new BusinessException(r.getMsg());
        }
        expert.setUserId(r.getData());
        //直接设置专家用户id
        expert.setUserId(expertVO.getUserId());
        this.updateById(expert);*/
        return res;
    }

    @Override
    public boolean delete(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, Integer state) {
        LambdaUpdateWrapper<Expert> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Expert::getExpertState, state);
        updateWrapper.eq(Expert::getId, id);
        return update(updateWrapper);
    }


    /**
     * 专家审批开始
     * @param variables
     */
    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer state = ExpertStateEnum.IN_APPROVAL.getState();
        Integer expertState = NumberConstant.ZERO;
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            state = ExpertStateEnum.APPROVE.getState();
            expertState = NumberConstant.ONE;
        }
        ExpertChange expertChange = expertChangeService.getById(businessId);
        if(expertChange!=null){
            expertChangeService.update(new LambdaUpdateWrapper<ExpertChange>()
                    .set(ExpertChange::getWfProcessId,processId)/* 流程id */
                    .set(ExpertChange::getExpertState,expertState)/* 启用状态 */
                    .set(ExpertChange::getState,state)/* 审批状态 */
                    .set(ExpertChange::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(ExpertChange::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(ExpertChange::getId,businessId));
            if(state == ExpertStateEnum.APPROVE.getState()){
                Expert expert = new Expert();
                BeanUtils.copyProperties(expertChange, expert);
                expert.setId(expertChange.getExpertId());
                super.updateById(expert);
            }
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getWfProcessId,processId)/* 流程id */
                    .set(Expert::getExpertState,expertState)/* 启用状态 */
                    .set(Expert::getState,state)/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId,expertChange.getExpertId()));
        }else{
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getWfProcessId,processId)/* 流程id */
                    .set(Expert::getExpertState,expertState)/* 启用状态 */
                    .set(Expert::getState,state)/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_ADD.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId,businessId));
        }
    }

    /**
     * 专家审批通过
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();

        ExpertChange expertChange = expertChangeService.getById(businessId);
        if(expertChange!=null){
            expertChangeService.update(new LambdaUpdateWrapper<ExpertChange>()
                    .set(ExpertChange::getWfProcessId,processId)/* 流程id */
                    .set(ExpertChange::getExpertState,NumberConstant.ONE)/* 启用状态 */
                    .set(ExpertChange::getState,ExpertStateEnum.APPROVE.getState())/* 审批状态 */
                    .set(ExpertChange::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(ExpertChange::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(ExpertChange::getId,businessId));
            Expert expert = new Expert();
            BeanUtils.copyProperties(expertChange, expert);
            expert.setId(expertChange.getExpertId());
            super.updateById(expert);
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getWfProcessId,processId)/* 流程id */
                    .set(Expert::getExpertState,NumberConstant.ONE)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.APPROVE.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId,expertChange.getExpertId()));
        }else{
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getWfProcessId,processId)/* 流程id */
                    .set(Expert::getExpertState,NumberConstant.ONE)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.APPROVE.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_ADD.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId,businessId));
        }
    }

    /**
     * 专家审批拒绝
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        ExpertChange expertChange = expertChangeService.getById(businessId);
        if(expertChange!=null){
            expertChangeService.update(new LambdaUpdateWrapper<ExpertChange>()
                    .set(ExpertChange::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(ExpertChange::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(ExpertChange::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(ExpertChange::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(ExpertChange::getId, businessId));
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId, expertChange.getExpertId()));
        }else{
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_ADD.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId, businessId));
        }
    }

    /**
     * 审批驳回到发起人
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        ExpertChange expertChange = expertChangeService.getById(businessId);
        if(expertChange!=null){
            expertChangeService.update(new LambdaUpdateWrapper<ExpertChange>()
                    .set(ExpertChange::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(ExpertChange::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(ExpertChange::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(ExpertChange::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(ExpertChange::getId, businessId));
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_CHANGE.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId, expertChange.getExpertId()));
        }else{
            super.update(new LambdaUpdateWrapper<Expert>()
                    .set(Expert::getExpertState,NumberConstant.ZERO)/* 启用状态 */
                    .set(Expert::getState,ExpertStateEnum.REJECT.getState())/* 审批状态 */
                    .set(Expert::getProcessType,ExpertProcessTypeEnum.EXPERT_ADD.getState())/* 流程类型 */
                    .set(Expert::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                    .eq(Expert::getId, businessId));
        }
    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        SysUser sysUser;
        ExpertChange expertChange = expertChangeService.getOne(new LambdaQueryWrapper<ExpertChange>()
                .eq(ExpertChange::getId,requestDTO.getBusinessId()));
        if(expertChange!=null){
            sysUser = systemUserService.getUserById(expertChange.getUserId());
        }else{
            Expert expert = getById(requestDTO.getBusinessId());
            sysUser = systemUserService.getUserById(expert.getUserId());
        }
        /* 根据组织获取对应的二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.initialize(requestDTO);
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        SysUser sysUser;
        ExpertChange expertChange = expertChangeService.getOne(new LambdaQueryWrapper<ExpertChange>()
                .eq(ExpertChange::getId,requestDTO.getBusinessId()));
        if(expertChange!=null){
            sysUser = systemUserService.getUserById(expertChange.getUserId());
        }else{
            Expert expert = getById(requestDTO.getBusinessId());
            sysUser = systemUserService.getUserById(expert.getUserId());
        }
        /* 根据组织获取对应的二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.listProcessLog(requestDTO);
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        SysUser sysUser;
        ExpertChange expertChange = expertChangeService.getOne(new LambdaQueryWrapper<ExpertChange>()
                .eq(ExpertChange::getId,(Serializable) variables.get("businessId")));
        if(expertChange!=null){
            sysUser = systemUserService.getUserById(expertChange.getUserId());
        }else{
            Expert expert = getById((Serializable) variables.get("businessId"));
            sysUser = systemUserService.getUserById(expert.getUserId());
        }
        /* 根据组织获取对应的二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        variables.put("companyId", org);/* 公司 二级单位 */
        variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        variables.put("parentProjectCode", org);/* 父项目编码(项目部) */

        return processService.auditProcessInstance(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying(),variables);
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        SysUser sysUser;
        ExpertChange expertChange = expertChangeService.getOne(new LambdaQueryWrapper<ExpertChange>()
                .eq(ExpertChange::getId,requestDTO.getBusinessId()));
        if(expertChange!=null){
            sysUser = systemUserService.getUserById(expertChange.getUserId());
        }else{
            Expert expert = getById(requestDTO.getBusinessId());
            sysUser = systemUserService.getUserById(expert.getUserId());
        }
        /* 根据组织获取对应的二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.loadTaskDef(requestDTO);
    }

}
