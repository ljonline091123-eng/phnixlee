package com.zhaocai.business.process.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zhaocai.business.common.enums.ProcessKeyEnum;
import com.zhaocai.business.common.enums.ProcessStateEnum;
import com.zhaocai.business.common.enums.RejectTaskKeyEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.BpmService;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.business.pub.service.ISystemUserService;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author ssy
 * @date 2024/7/30 10:21
 */
@Slf4j
@Service
public class BPMProcessService implements IBPMProcessService {

    @Autowired
    private BpmService bpmService;
    @Autowired
    private IMinProjectService minProjectService;
    @Autowired
    private ISystemUserService systemUserService;
    @Autowired
    private UnderlingSystemService underlingSystemService;

    /**
     * 发起流程
     *
     * @param processKey
     * @param variables
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String startProcessInstance(String processKey, Map<String, Object> variables) {
        log.info("[发起流程startProcessInstance] processKey:{}", processKey);
        log.info("[发起流程startProcessInstance] variables:{}", variables);
        //1.实现调用第三方的提交接口
        BpmSubmitRequestDTO requestDTO = new BpmSubmitRequestDTO();
        String customProcessKey = variables.get("customProcessKey") == null ? null : variables.get("customProcessKey").toString();
        requestDTO.setBusinessId(variables.get("businessId").toString());
        requestDTO.setProcessKey(StrUtil.isBlank(customProcessKey) ? processKey : customProcessKey);
        requestDTO.setBusinessContent(
                variables.get("businessContent") == null ? null : variables.get("businessContent").toString());
        //variables.get("businessTitle").toString()
        // 目前就暂时按这里这样统一的叫法
        requestDTO.setBusinessTitle("流程审批");
        requestDTO.setState(variables.get("detailUrl") == null ? IdUtil.getSnowflakeNextId() + "" : variables.get("detailUrl").toString());
        requestDTO.setUserObj(variables.get("userObj") == null ? null : variables.get("userObj").toString());
        String operateComment = variables.get("operateComment") == null ? null : variables.get("operateComment").toString();
        requestDTO.setOperateComment(operateComment);
        /* 下一个审批用户id */
        System.out.println("[下一个审批用户id]" + variables.get("nextAuditUserId"));
        requestDTO.setNextAuditUserId(variables.get("nextAuditUserId") == null ? null : variables.get("nextAuditUserId").toString());

        /** propertyList:运行时属性对象：项目部（parentProjectCode），责任单位（responsibilityDeptId），公司（companyId），集团（groupId），合同类型（contractType），价格(contractMoney) */
        /* 集团是顶级，公司是二级，责任单位是三级，项目部是四级 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();

        /* 业务用到的 区分集团账号唯一编码 */
        if (variables.get("groupId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", variables.get("groupId").toString());/* 1000000000 */
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 责任单位（responsibilityDeptId） */
        if (variables.get("responsibilityDeptId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", variables.get("responsibilityDeptId").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 公司（companyId） */
        if (variables.get("companyId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", variables.get("companyId").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 项目部（parentProjectCode） */
        if (variables.get("parentProjectCode") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", variables.get("parentProjectCode").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 合同类型 ：劳务分包 专业分包 购买材料 租赁材料 租赁机械（设备） 其他 */
        if (variables.get("contractType") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "contractType", variables.get("contractType").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 合同签订金额(含税) */
        if (variables.get("contractMoney") != null) {
            /** BigDecimal类型 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "contractMoney", new BigDecimal(variables.get("contractMoney").toString()));
            requestDTO.setPropertyList(propertyList);
        }
        /* 最小核算项目编码 */
        String projectCode = variables.get("projectCode") == null ? null : variables.get("projectCode").toString();
        if (StrUtil.isNotBlank(projectCode)) {
            /* 最小核算项目 */
            MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(projectCode);
            if (null != minProjectVO) {
                PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", minProjectVO.getParentCode());
                PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", minProjectVO.getDutyUnit());
                PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);
                PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId()));
                requestDTO.setPropertyList(propertyList);
            }
        }

        BpmSubmitResponseDTO responseDTO = bpmService.submit(requestDTO);
        String processId = responseDTO.getProcessId();
        variables.put("processId", processId);

        String completedFlag = "";
        if (ProcessStateEnum.COMPLETED.getValue().equals(responseDTO.getProcessStatus())) {
            completedFlag = ProcessStateEnum.COMPLETED.getDesc();
        }
        variables.put("completedFlag", completedFlag);

        //2.处理业务的service
        getProcessBusinessService(processKey).processStart(variables);

        return processId;
    }


    /**
     * 审批流程
     *
     * @param processKey
     * @param variables
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String auditProcessInstance(String processKey, Map<String, Object> variables) {
        //1.实现调用第三方的审批接口
        BpmAuditRequestDTO requestDTO = new BpmAuditRequestDTO();
        requestDTO.setProcessId(variables.get("processId").toString());
        requestDTO.setBusinessId(variables.get("businessId").toString());
        requestDTO.setOperateComment(variables.get("operateComment").toString());
        requestDTO.setCurTaskId(variables.get("curTaskId").toString());
        /* 下一个审批用户id */
        System.out.println("[下一个审批用户id]" + variables.get("nextAuditUserId"));
        requestDTO.setNextAuditUserId(variables.get("nextAuditUserId") == null ? null : variables.get("nextAuditUserId").toString());

        /** propertyList:运行时属性对象：项目部（parentProjectCode），责任单位（responsibilityDeptId），公司（companyId），集团（groupId），合同类型（contractType），价格(contractMoney) */
        /* 集团是顶级，公司是二级，责任单位是三级，项目部是四级 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();

        /* 业务用到的 区分集团账号唯一编码 */
        if (variables.get("groupId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", variables.get("groupId").toString());/* 1000000000 */
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 责任单位（responsibilityDeptId） */
        if (variables.get("responsibilityDeptId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", variables.get("responsibilityDeptId").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 公司（companyId） */
        if (variables.get("companyId") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", variables.get("companyId").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 业务用到的 项目部（parentProjectCode） */
        if (variables.get("parentProjectCode") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", variables.get("parentProjectCode").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 合同类型 ：劳务分包 专业分包 购买材料 租赁材料 租赁机械（设备） 其他 */
        if (variables.get("contractType") != null) {
            PropertyListRequestDTO.addPropertyToList(propertyList, "contractType", variables.get("contractType").toString());
            requestDTO.setPropertyList(propertyList);
        }
        /* 合同签订金额(含税) */
        if (variables.get("contractMoney") != null) {
            /** BigDecimal类型 */
            PropertyListRequestDTO.addPropertyToList(propertyList, "contractMoney", new BigDecimal(variables.get("contractMoney").toString()));
            requestDTO.setPropertyList(propertyList);
        }

        boolean pass = (boolean) variables.get("pass");
        requestDTO.setPass(pass);
        if (!pass) {
            requestDTO.setRejectTaskKey(variables.get("rejectTaskKey").toString());
        }
        BpmAuditResponseDTO responseDTO = bpmService.audit(requestDTO);
        System.out.println("-------------pass");
        System.out.println(pass);
        System.out.println("-------------responseDTO");
        System.out.println(responseDTO);
        //流程状态
        String processStatus = responseDTO.getProcessStatus();
        if (pass) {
            //完成状态
            if (ProcessStateEnum.COMPLETED.getValue().equals(processStatus)) {
                System.out.println("-------------ProcessStateEnum.COMPLETED.getValue()");
                getProcessBusinessService(processKey).processAuditPass(variables);
            }
        } else {
            //驳回到发起人 状态：自由态
            if (ProcessStateEnum.FREEDOM.getValue().equals(processStatus)) {
                getProcessBusinessService(processKey).processAuditFreedom(variables);
            }

            //驳回
            if (ProcessStateEnum.REJECTED.getValue().equals(processStatus)) {
                //驳回到发起人 状态：自由态
                if (requestDTO.getRejectTaskKey().equals(RejectTaskKeyEnum.SUBMIT.getDesc())) {
                    getProcessBusinessService(processKey).processAuditFreedom(variables);
                } else {
                    getProcessBusinessService(processKey).processAuditReject(variables);
                }
            }

        }
        return variables.get("processId").toString();
    }

    /**
     * 撤销流程
     *
     * @param processKey
     * @param variables
     * @return
     */
    @Override
    public String revokedProcessInstance(String processKey, Map<String, Object> variables) {
        //1.实现调用第三方的审批接口
        BpmRevokeRequestDTO requestDTO = new BpmRevokeRequestDTO();
        requestDTO.setProcessId(variables.get("processId").toString());
        BpmRevokeResponseDTO responseDTO = bpmService.revoke(requestDTO);
        //流程状态
        String processStatus = responseDTO.getProcessStatus();
        if (ProcessStateEnum.REVOKED.getValue().equals(processStatus)) {
            getProcessBusinessService(processKey).processAuditRevoke(variables);
        }
        return variables.get("processId").toString();
    }

    /**
     * 撤回流程
     *
     * @param processKey
     * @param variable
     * @return
     */
    @Override
    public String revokeProcess(String processKey, Map<String, Object> variable) {
        BpmLoadTaskDefRequestDTO loadTask = new BpmLoadTaskDefRequestDTO();
        loadTask.setBusinessId(variable.get("businessId").toString());
        loadTask.setProcessId(variable.get("processId").toString());


        // 获取流程定义信息
        List<BpmLoadTaskDefResponseDTO> loadTaskDefList = bpmService.loadTaskDef(loadTask);

        if (!ObjectUtils.isEmpty(loadTaskDefList)) {
            // 找到待审位置：即第一个完成状态为false的位置
            OptionalInt indexOpt = IntStream.range(0, loadTaskDefList.size())
                    .filter(i -> !loadTaskDefList.get(i).isCompleted())
                    .findFirst();
            // 判断待审是否存在
            if (!indexOpt.isPresent() || indexOpt.getAsInt() == 0) {
                // 所有结点都已完成或都未完成(未提交)
                throw new ParamValidateException("流程已结束或开始");
            } else {
                int index = indexOpt.getAsInt();
                // 可撤回节点：待审节点的前一个节点
                BpmLoadTaskDefResponseDTO loadTaskDef = loadTaskDefList.get(index - 1);
                List<UserList> userList = loadTaskDef.getUserList();
                String userId = String.valueOf(SecurityUtils.getThridUserId());
                List<String> user = userList.stream().filter(u -> u.getUserId().equals(userId))
                        .map(UserList::getUserId).collect(Collectors.toList());
//                Optional<UserList> user = loadTaskDef.getUserList().stream()
//                        .filter(u -> u.getUserId().equals(String.valueOf(SecurityUtils.getUserId())))
//                        .findFirst();
                // 判断可撤回的人员和当前登陆人是否是同一个人
                if (!user.isEmpty()) {
                    BpmInitializeRequestDTO initializeRequestDTO = BeanCopierUtil.copyBean(loadTask, BpmInitializeRequestDTO.class);
                    BpmInitializeResponseDTO initialize = bpmService.initialize(initializeRequestDTO);
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("businessId", loadTask.getBusinessId());
                    variables.put("curTaskId", initialize.getCurTaskId());
                    variables.put("operateComment", "撤回");
                    variables.put("pass", false);
                    variables.put("processId", loadTask.getProcessId());
                    if (index - 1 == 0) {
                        // 发起人撤回
                        revokedProcessInstance(processKey, variables);
                    } else {
                        variables.put("rejectTaskKey", loadTaskDef.getNodeKey());
                        // 其他节点撤回
                        auditProcessInstance(processKey, variables);
                    }
                } else {
                    String userName = userList.stream().map(UserList::getUserName).collect(Collectors.joining(", "));
                    throw new ParamValidateException("当前登陆人不能进行撤回,当前登陆人：" + SecurityUtils.getLoginUserNickName() + ",可撤回人员：" + userName);
                }
            }
        } else {
            throw new ParamValidateException("未找到对应流程");
        }
        return variable.get("processId").toString();
    }


    @Override
    public String getOrg(String org){
        String result = null;
        /* 根据组织获取对应的二级单位 */
        String orgTwo = underlingSystemService.getL2OrgByOrgId(org);
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(org);
        /* 获取所有流程 */
        List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
        if (listCataLogDTOS != null) {
            /* 判断二级单位流程是否存在 */
            ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(orgTwo)).findFirst().orElse(null);
            if (cataLogDTOTwo != null) {
                /* 赋值使用二级单位 */
                result = orgTwo;
            }
            if (orgThree != null) {
                /* 判断三级单位流程是否存在 */
                ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(orgThree)).findFirst().orElse(null);
                if (cataLogDTOThree != null) {
                    /* 赋值使用三级单位 */
                    result = orgThree;
                }
            }
        }
        return result;
    }

    @Override
    public String getOrgByUserId(String userId){
        SysUser sysUser = systemUserService.getUserById(Long.parseLong(userId));
        if(sysUser==null)return null;
        return getOrg(sysUser.getThridOrgId());
    }

    /**
     * 初始化接口
     */
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return ResultData.data(bpmService.initialize(requestDTO));
    }

    /**
     * 流程操作日志列表接口
     */
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return ResultData.data(bpmService.listProcessLog(requestDTO));
    }

    /**
     * 加载定义接口
     */
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return ResultData.data(bpmService.loadTaskDef(requestDTO));
    }

    @Override
    public String revokeVendorProcess(String processKey, Map<String, Object> variable) {
        BpmLoadTaskDefRequestDTO loadTask = new BpmLoadTaskDefRequestDTO();
        loadTask.setBusinessId(variable.get("businessId").toString());
        loadTask.setProcessId(variable.get("processId").toString());


        // 获取流程定义信息
        List<BpmLoadTaskDefResponseDTO> loadTaskDefList = bpmService.loadTaskDef(loadTask);

        if (!ObjectUtils.isEmpty(loadTaskDefList)) {
            // 找到待审位置：即第一个完成状态为false的位置
            OptionalInt indexOpt = IntStream.range(0, loadTaskDefList.size())
                    .filter(i -> !loadTaskDefList.get(i).isCompleted())
                    .findFirst();
            // 判断待审是否存在
            if (!indexOpt.isPresent() || indexOpt.getAsInt() == 0) {
                // 所有结点都已完成或都未完成(未提交)
                throw new ParamValidateException("流程已结束或开始");
            } else {
                int index = indexOpt.getAsInt();
                // 可撤回节点：待审节点的前一个节点
                BpmLoadTaskDefResponseDTO loadTaskDef = loadTaskDefList.get(index - 1);
                List<UserList> userList = loadTaskDef.getUserList();
                String userId = String.valueOf(SecurityUtils.getThridUserId());
                List<String> user = userList.stream().filter(u -> u.getUserId().equals(userId))
                        .map(UserList::getUserId).collect(Collectors.toList());
//                Optional<UserList> user = loadTaskDef.getUserList().stream()
//                        .filter(u -> u.getUserId().equals(String.valueOf(SecurityUtils.getUserId())))
//                        .findFirst();
                // 判断可撤回的人员和当前登陆人是否是同一个人 或者 第一步是招采
                if (!user.isEmpty() || (index - 1 == 0 && userList != null && userList.size() > 0 && "zc-anonymous".equals(userList.get(0).getUserId()))) {
                    BpmInitializeRequestDTO initializeRequestDTO = BeanCopierUtil.copyBean(loadTask, BpmInitializeRequestDTO.class);
                    BpmInitializeResponseDTO initialize = bpmService.initialize(initializeRequestDTO);
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("businessId", loadTask.getBusinessId());
                    variables.put("curTaskId", initialize.getCurTaskId());
                    variables.put("operateComment", "撤回");
                    variables.put("pass", false);
                    variables.put("processId", loadTask.getProcessId());
                    if (index - 1 == 0) {
                        // 发起人撤回
                        revokedProcessInstance(processKey, variables);
                    } else {
                        variables.put("rejectTaskKey", loadTaskDef.getNodeKey());
                        // 其他节点撤回
                        auditProcessInstance(processKey, variables);
                    }
                } else {
                    String userName = userList.stream().map(UserList::getUserName).collect(Collectors.joining(", "));
                    throw new ParamValidateException("当前登陆人不能进行撤回,当前登陆人：" + SecurityUtils.getLoginUserNickName() + ",可撤回人员：" + userName);
                }
            }
        } else {
            throw new ParamValidateException("未找到对应流程");
        }
        return variable.get("processId").toString();
    }


    /* 业务流程实现对象 */
    private IProcessBusinessBaseService getProcessBusinessService(String processKey) {
        Class<IProcessBusinessBaseService> handlerServiceClass = PROCESS_BUSINESS_MAP.get(processKey);
        if (handlerServiceClass == null) {
            throw new ParamValidateException(processKey + "对应的业务类为空,请在[IBPMProcessService#PROCESS_BUSINESS_MAP]中配置...");
        }
        IProcessBusinessBaseService businessService = SpringUtil.getBean(handlerServiceClass);
        if (businessService == null) {
            throw new ParamValidateException(processKey + "对应的业务类为空,请在[IBPMProcessService#PROCESS_BUSINESS_MAP]中配置...");
        }
        return businessService;
    }

}
