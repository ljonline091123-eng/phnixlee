package com.zhaocai.flowable.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.flowable.common.constant.ProcessConstants;
import com.zhaocai.flowable.common.enums.FlowComment;
import com.zhaocai.flowable.domain.SysProcessTitle;
import com.zhaocai.flowable.domain.dto.FlowProcDefDto;
import com.zhaocai.flowable.factory.FlowServiceFactory;
import com.zhaocai.flowable.mapper.FlowDeployMapper;
import com.zhaocai.flowable.mapper.SysDeployFormMapper;
import com.zhaocai.flowable.mapper.SysFormMapper;
import com.zhaocai.flowable.service.IFlowDefinitionService;
import com.zhaocai.system.api.domain.SysUser;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 流程定义
 *
 * @author Acecehgnui
 */
@Service
public class FlowDefinitionServiceImpl extends FlowServiceFactory implements IFlowDefinitionService {

    @Resource
    private FlowDeployMapper flowDeployMapper;

    @Resource
    private SysDeployFormMapper sysDeployFormMapper;

    @Resource
    private SysFormMapper sysFormMapper;

    private static final String BPMN_FILE_SUFFIX = ".bpmn";

    @Override
    public boolean exist(String processDefinitionKey) {
        ProcessDefinitionQuery processDefinitionQuery
                = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey);
        long count = processDefinitionQuery.count();
        return count > 0;
    }


    /**
     * 流程定义列表
     *
     * @param name 参数
     * @return 流程定义分页列表数据
     */
    @Override
    public List<FlowProcDefDto> list(String name) {
        return flowDeployMapper.selectDeployList(name);
    }

    /**
     * 各个流程定义最新版本列表
     *
     * @param name 参数
     * @return 流程定义分页列表数据
     */
    @Override
    public List<FlowProcDefDto> listLast(String name) {
        return flowDeployMapper.selectDeployListLast(
                name
        );
    }


    /**
     * 导入流程文件
     *
     * @param name
     * @param category
     * @param in
     */
    @Override
    public void importFile(String name, String category, InputStream in) {
        Deployment deploy = repositoryService.createDeployment().addInputStream(name + BPMN_FILE_SUFFIX, in).name(name).category(category).deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
        repositoryService.setProcessDefinitionCategory(definition.getId(), category);

    }

    /**
     * 读取xml
     *
     * @param deployId 部署ID
     */
    @Override
    public String readXml(String deployId) throws IOException {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        if (Objects.isNull(definition)) {
            throw new CheckedException("读取不到流程xml文件");
        }
        InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName());
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * 读取xml
     *
     * @param deployId
     */
    @Override
    public InputStream readImage(String deployId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        //获得图片流
        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        //输出为图片
        return diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                Collections.emptyList(),
                Collections.emptyList(),
                "宋体",
                "宋体",
                "宋体",
                null,
                1.0,
                false);

    }

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param procDefId 流程定义Id
     * @param variables 流程变量，表单参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startProcessInstanceById(String procDefId, Map<String, Object> variables) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId)
                    .latestVersion().singleResult();
            if (Objects.nonNull(processDefinition) && processDefinition.isSuspended()) {
                throw new CheckedException("流程已被挂起,请先激活流程");
            }
            // 设置流程发起人Id到流程中
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            identityService.setAuthenticatedUserId(sysUser.getUserId().toString());
            variables.put(ProcessConstants.PROCESS_INITIATOR, sysUser.getUserId().toString());
            variables.put(ProcessConstants.PROCESS_APPROVAL, "");
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, variables);
            /**
             * 将流程标题信息存入
             */
            int i = flowDeployMapper.insertSysProcessTitle(new SysProcessTitle(processInstance.getId(), String.valueOf(variables.get("processTitle"))));
            if (i <= 0) {
                throw new CheckedException("流程标题添加失败");
            }
            // 给第一步申请人节点设置任务执行人和意见 todo:第一个节点不设置为申请人节点有点问题
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
            if (Objects.nonNull(task)) {
                //往意见表增加记录
                taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), FlowComment.NORMAL.getType(), sysUser.getNickName() + "发起流程申请");
                //往附件表里增加记录,注意:这里会自动往意见表里再增加记录, type值为event
                if (Objects.nonNull(variables.get("files")) && !variables.get("files").equals("null")) {
                    JSONArray jo = (JSONArray) JSON.parse((String) variables.get("files"));
                    ;
                    for (Object o : jo) {
                        JSONObject js = (JSONObject) o;
                        taskService.createAttachment(FlowComment.NORMAL.getType(), task.getId(), processInstance.getProcessInstanceId()
                                , js.getString("name"), sysUser.getNickName() + "上传的附件", js.getString("url"));
                    }

                }
                taskService.complete(task.getId(), variables);
            }
            return true;
        } catch (FlowableException e) {
            e.printStackTrace();
            throw new CheckedException("流程启动错误:" + e.getMessage());
        }
    }

    /**
     * 根据流程定义ID启动流程实例返回流程实例id
     *
     * @param procDefIdOld 流程定义Id
     * @param variables    流程变量，表单参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startProcessInstanceById1(String procDefIdOld, Map<String, Object> variables) {
        try {
            String procDefId = getProcDefIdBySign(procDefIdOld);
            if (StringUtils.isEmpty(procDefId)) {
                throw new CheckedException("未找到匹配流程，请检查后重试");
            }
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId)
                    .latestVersion().singleResult();
            if (Objects.nonNull(processDefinition) && processDefinition.isSuspended()) {
                throw new CheckedException("流程已被挂起,请先激活流程");
            }

            String nickName = "";
            if (SecurityUtils.getLoginUser() != null) {
                // 设置流程发起人Id到流程中
                SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
                identityService.setAuthenticatedUserId(sysUser.getUserId().toString());
                variables.put(ProcessConstants.PROCESS_INITIATOR, sysUser.getUserId().toString());
                nickName = sysUser.getNickName();
            } else {
                nickName = variables.get("nickName").toString();
                identityService.setAuthenticatedUserId(variables.get("INITIATOR").toString());
                variables.put(ProcessConstants.PROCESS_INITIATOR, variables.get("INITIATOR").toString());
            }

            variables.put(ProcessConstants.PROCESS_APPROVAL, "");

            //启动流程实例
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, variables.get("businessId") + "", variables);
            /**
             * 将流程标题信息存入
             */
            int i = flowDeployMapper.insertSysProcessTitle(new SysProcessTitle(processInstance.getId(), String.valueOf(variables.get("businessTitle"))));
            if (i <= 0) {
                throw new CheckedException("流程标题添加失败");
            }
            // 给第一步申请人节点设置任务执行人和意见 todo:第一个节点不设置为申请人节点有点问题
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
            if (Objects.nonNull(task)) {
                //往意见表增加记录
                taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), FlowComment.INITIATE.getType(), nickName + "发起流程申请");
                //往附件表里增加记录,注意:这里会自动往意见表里再增加记录, type值为event
                if (Objects.nonNull(variables.get("files")) && !variables.get("files").equals("null")) {
                    JSONArray jo = (JSONArray) JSON.parse((String) variables.get("files"));
                    ;
                    for (Object o : jo) {
                        JSONObject js = (JSONObject) o;
                        taskService.createAttachment(FlowComment.NORMAL.getType(), task.getId(), processInstance.getProcessInstanceId()
                                , js.getString("name"), nickName + "上传的附件", js.getString("url"));
                    }

                }
                taskService.complete(task.getId(), variables);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("flag", "true");
            map.put("instanceId", processInstance.getProcessInstanceId());
            //根据流程实例判断流程是否结束
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();

            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();

            boolean isCompleted = historicInstance != null && historicInstance.getEndTime() != null;

            map.put("isEnded", isCompleted);
            return map;
        } catch (FlowableException e) {
            e.printStackTrace();
            throw new CheckedException("流程启动错误:" + e.getMessage());
        }
    }

    /**
     * 根据流程定义ID启动流程实例返回流程实例id
     *
     * @param procDefId 流程定义Id
     * @param variables 流程变量，表单参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startProcessInstanceById2(String procDefId, Map<String, Object> variables) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId)
                    .latestVersion().singleResult();
            if (Objects.nonNull(processDefinition) && processDefinition.isSuspended()) {
                throw new CheckedException("流程已被挂起,请先激活流程");
            }
            // 设置流程发起人Id到流程中
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            identityService.setAuthenticatedUserId(sysUser.getUserId().toString());
            variables.put(ProcessConstants.PROCESS_INITIATOR, sysUser.getUserId().toString());
            variables.put(ProcessConstants.PROCESS_APPROVAL, "");
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, variables);
            /**
             * 将流程标题信息存入
             */
            int i = flowDeployMapper.insertSysProcessTitle(new SysProcessTitle(processInstance.getId(), String.valueOf(variables.get("processTitle"))));
            if (i <= 0) {
                throw new CheckedException("流程标题添加失败");
            }
            // 给第一步申请人节点设置任务执行人和意见 todo:第一个节点不设置为申请人节点有点问题
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
            if (Objects.nonNull(task)) {
                //往意见表增加记录
                taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), FlowComment.NORMAL.getType(), sysUser.getNickName() + "发起流程申请");
                //往附件表里增加记录,注意:这里会自动往意见表里再增加记录, type值为event
                if (Objects.nonNull(variables.get("files")) && !variables.get("files").equals("null")) {
                    JSONArray jo = (JSONArray) JSON.parse((String) variables.get("files"));
                    ;
                    for (Object o : jo) {
                        JSONObject js = (JSONObject) o;
                        taskService.createAttachment(FlowComment.NORMAL.getType(), task.getId(), processInstance.getProcessInstanceId()
                                , js.getString("name"), sysUser.getNickName() + "上传的附件", js.getString("url"));
                    }

                }
                //taskService.complete(task.getId(), variables);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("flag", "true");
            map.put("instanceId", processInstance.getProcessInstanceId());
            return map;
        } catch (FlowableException e) {
            e.printStackTrace();
            throw new CheckedException("流程启动错误:" + e.getMessage());
        }
    }

    /**
     * 通过流程标识key最新版本流程定义id
     *
     * @param procDefKey 流程标识key
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getByProcDefKey(String procDefKey) {
        ProcessDefinition processDefinition = (ProcessDefinition) repositoryService.createProcessDefinitionQuery().processDefinitionKey(procDefKey).latestVersion().singleResult();
        if (Objects.nonNull(processDefinition)) {
            return processDefinition.getId();
        } else {
            throw new CheckedException("未找到对应流程定义id");
        }
    }

    /**
     * 根据流程定义ID更新流程实例中的流程变量
     *
     * @param procDefId 实例ID
     * @param variables 流程变量
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editProcessInstanceById(String procDefId, Map<String, Object> variables) {
        /*注意:
        1.只要在act_ru_*表中存在,那么在act_hi_*表中一定会存在,反之,不一定会存在.
        2.key名相同,值会被覆盖.例:比如说key为"save-key",值为1,第二次存入"save-key",值为2,之后的值一直都会是2*/
        runtimeService.setVariables(procDefId, variables);
        return true;
    }


    /**
     * 激活或挂起流程定义
     *
     * @param state    状态
     * @param deployId 流程部署ID
     */
    @Override
    public void updateState(Integer state, String deployId) {
        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        // 激活
        if (state == 1) {
            repositoryService.activateProcessDefinitionById(procDef.getId(), true, DateUtils.getNowDate());
        }
        // 挂起
        if (state == 2) {
            repositoryService.suspendProcessDefinitionById(procDef.getId(), true, DateUtils.getNowDate());
        }
    }


    /**
     * 删除流程定义
     *
     * @param deployId 流程部署ID act_ge_bytearray 表中 deployment_id值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String[] deployId) {
        //删除流程绑定的表单信息
        sysDeployFormMapper.deleteSysDeployFormByDeployIds(deployId);
        for (String d : deployId) {
            // true 允许级联删除 ,不设置会导致数据库外键关联异常
            repositoryService.deleteDeployment(d, true);
        }
    }


    /**
     * 根据流程标识key获取流程定义id
     *
     * @param processDefinitionKey
     * @return
     */
    public String getProcDefIdBySign(String processDefinitionKey) {
        return sysFormMapper.getProcDefIdBySign(processDefinitionKey);
    }

}
