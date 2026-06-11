package com.zhaocai.flowable.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.flowable.common.constant.ProcessConstants;
import com.zhaocai.flowable.common.enums.FlowComment;
import com.zhaocai.flowable.domain.SysForm;
import com.zhaocai.flowable.domain.SysProcessTitle;
import com.zhaocai.flowable.domain.SysTaskCc;
import com.zhaocai.flowable.domain.dto.*;
import com.zhaocai.flowable.domain.vo.FlowTaskVo;
import com.zhaocai.flowable.factory.FlowServiceFactory;
import com.zhaocai.flowable.flow.CustomProcessDiagramGenerator;
import com.zhaocai.flowable.flow.FindNextNodeUtil;
import com.zhaocai.flowable.flow.FlowableUtils;
import com.zhaocai.flowable.mapper.FlowDeployMapper;
import com.zhaocai.flowable.mapper.FlowHistericTaskMapper;
import com.zhaocai.flowable.mapper.SysTaskCcMapper;
import com.zhaocai.flowable.service.IFlowTaskService;
import com.zhaocai.flowable.service.ISysDeployFormService;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysRole;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cmd.AddMultiInstanceExecutionCmd;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Acecehgnui
 **/
@Service
public class FlowTaskServiceImpl extends FlowServiceFactory implements IFlowTaskService {

    @Resource
    private RemoteUserService remoteuserservice;

    @Resource
    private ISysDeployFormService sysInstanceFormService;

    @Resource
    private FlowDeployMapper flowDeployMapper;

    @Resource
    private FlowHistericTaskMapper flowHistericTaskMapper;

    @Resource
    private SysTaskCcMapper sysTaskCcMapper;


    /**
     * 任务归还
     * 被委派人完成任务之后，将任务归还委派人
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveTask(FlowTaskVo flowTaskVo) {
        taskService.resolveTask(flowTaskVo.getTaskId());
    }


    /**
     * 多实例加签
     * act_ru_task、act_ru_identitylink各生成一条记录
     */
    @Override
    public void addMultiInstanceExecution(FlowTaskVo flowTaskVo) {
        managementService.executeCommand(new AddMultiInstanceExecutionCmd(flowTaskVo.getDefId(), flowTaskVo.getInstanceId(), flowTaskVo.getVariables()));
    }


    @Override
    public void deleteMultiInstanceExecution(FlowTaskVo flowTaskVo) {

    }

    @Override
    public Map<String, Object> flowXmlAndNode(String procInsId, String deployId) throws IOException {
        List<FlowViewerDto> flowViewerList = new ArrayList<>();
        // 获取已经完成的节点
        List<HistoricActivityInstance> listFinished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInsId)
                .finished()
                .list();

        // 保存已经完成的流程节点编号
        listFinished.forEach(s -> {
            FlowViewerDto flowViewerDto = new FlowViewerDto();
            flowViewerDto.setKey(s.getActivityId());
            flowViewerDto.setCompleted(true);
            flowViewerList.add(flowViewerDto);
        });

        // 获取代办节点
        List<HistoricActivityInstance> listUnFinished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInsId)
                .unfinished()
                .list();

        // 保存需要代办的节点编号
        listUnFinished.forEach(s -> {
            FlowViewerDto flowViewerDto = new FlowViewerDto();
            flowViewerDto.setKey(s.getActivityId());
            flowViewerDto.setCompleted(false);
            flowViewerList.add(flowViewerDto);
        });
        Map<String, Object> result = new HashMap<>();
        // xmlData 数据
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName());
        String xmlData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        result.put("nodeData", flowViewerList);
        result.put("xmlData", xmlData);
        return result;
    }


    /**
     * 单个完成任务
     *
     * @param taskVo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> complete(FlowTaskVo taskVo) {
        Map<String, Object> map = new HashMap<>();
        Task task = taskService.createTaskQuery().taskId(taskVo.getTaskId()).singleResult();
        if (Objects.isNull(task)) {
            throw new CheckedException("任务不存在");
        }
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            taskService.addComment(taskVo.getTaskId(), taskVo.getInstanceId(), FlowComment.DELEGATE.getType(), taskVo.getComment());
            taskService.resolveTask(taskVo.getTaskId(), taskVo.getValues());
        } else {
            // 获取当前任务对应的流程节点定义
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            FlowNode currentNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            boolean pd = true;
            // 检查当前节点的出口是否指向结束事件
            List<SequenceFlow> outgoingFlows = currentNode.getOutgoingFlows();
            for (SequenceFlow flow : outgoingFlows) {
                FlowElement targetElement = flow.getTargetFlowElement();
                if (targetElement instanceof EndEvent) {
                    taskService.addComment(taskVo.getTaskId(), taskVo.getInstanceId(), FlowComment.COMPLETE.getType(), taskVo.getComment());
                    pd = false;
                }
            }
            if (pd) {
                taskService.addComment(taskVo.getTaskId(), taskVo.getInstanceId(), FlowComment.NORMAL.getType(), taskVo.getComment());
            }
            Long userId = SecurityUtils.getLoginUser().getSysUser().getUserId();
            taskService.setAssignee(taskVo.getTaskId(), userId.toString());
            //更新全局变量
            taskService.setVariables(taskVo.getTaskId(), taskVo.getVariables());
            taskService.complete(taskVo.getTaskId(), taskVo.getValues());
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(taskVo.getInstanceId())
                .singleResult();
        if (instance == null) {
            map.put("processStatus", "4");
        }
        return map;
    }

    /**
     * 批量完成任务
     *
     * @param taskIds 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean batchComplete(String[] taskIds) {
        Long userId = SecurityUtils.getLoginUser().getSysUser().getUserId();
        for (String ts : taskIds) {
            taskService.setAssignee(ts, userId.toString());
            taskService.complete(ts);
        }
        return true;
    }

    /**
     * 抄送任务
     *
     * @param task 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean courtesyCopy(FlowTaskVo task) {
        //根据任务id匹配,若已存在任务id,便覆盖,不存在,则插入
        Integer count = sysTaskCcMapper.selectSysTaskCcCountByinstanceId(task.getInstanceId());
        if (count > 0) {
            sysTaskCcMapper.deleteSysTaskCcByinstanceId(task.getInstanceId());
        }
        List<SysTaskCc> ccList = Arrays.stream(task.getUserId().split(",")).map(s -> new SysTaskCc(task.getInstanceId(), s)).collect(Collectors.toList());

        //在此处可发送企业微信消息等通知,自行结合业务实现

        return sysTaskCcMapper.insertSysTaskCc(ccList) > 0;
    }

    /**
     * 驳回任务
     *
     * @param flowTaskVo 参数
     */
    @Override
    public void taskReject(FlowTaskVo flowTaskVo) {
        if (taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult().isSuspended()) {
            throw new CheckedException("任务处于挂起状态!");
        }
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    // 获取节点信息
                    source = flowElement;
                }
            }
        }

        // 目的获取所有跳转到的节点 targetIds
        // 获取当前节点的所有父级用户任务节点
        // 深度优先算法思想：延边迭代深入
        List<UserTask> parentUserTaskList = FlowableUtils.iteratorFindParentUserTasks(source, null, null);
        if (parentUserTaskList == null || parentUserTaskList.size() == 0) {
            throw new CheckedException("当前节点为初始任务节点，不能驳回");
        }
        // 获取活动 ID 即节点 Key
        List<String> parentUserTaskKeyList = new ArrayList<>();
        parentUserTaskList.forEach(item -> parentUserTaskKeyList.add(item.getId()));
        // 获取全部历史节点活动实例，即已经走过的节点历史，数据采用开始时间升序
        List<HistoricTaskInstance> historicTaskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).orderByHistoricTaskInstanceStartTime().asc().list();
        // 数据清洗，将回滚导致的脏数据清洗掉
        List<String> lastHistoricTaskInstanceList = FlowableUtils.historicTaskInstanceClean(allElements, historicTaskInstanceList);
        // 此时历史任务实例为倒序，获取最后走的节点
        List<String> targetIds = new ArrayList<>();
        // 循环结束标识，遇到当前目标节点的次数
        int number = 0;
        StringBuilder parentHistoricTaskKey = new StringBuilder();
        for (String historicTaskInstanceKey : lastHistoricTaskInstanceList) {
            // 当会签时候会出现特殊的，连续都是同一个节点历史数据的情况，这种时候跳过
            if (parentHistoricTaskKey.toString().equals(historicTaskInstanceKey)) {
                continue;
            }
            parentHistoricTaskKey = new StringBuilder(historicTaskInstanceKey);
            if (historicTaskInstanceKey.equals(task.getTaskDefinitionKey())) {
                number++;
            }
            // 在数据清洗后，历史节点就是唯一一条从起始到当前节点的历史记录，理论上每个点只会出现一次
            // 在流程中如果出现循环，那么每次循环中间的点也只会出现一次，再出现就是下次循环
            // number == 1，第一次遇到当前节点
            // number == 2，第二次遇到，代表最后一次的循环范围
            if (number == 2) {
                break;
            }
            // 如果当前历史节点，属于父级的节点，说明最后一次经过了这个点，需要退回这个点
            if (parentUserTaskKeyList.contains(historicTaskInstanceKey)) {
                targetIds.add(historicTaskInstanceKey);
            }
        }


        // 目的获取所有需要被跳转的节点 currentIds
        // 取其中一个父级任务，因为后续要么存在公共网关，要么就是串行公共线路
        UserTask oneUserTask = parentUserTaskList.get(0);
        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需驳回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(oneUserTask, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));


        // 规定：并行网关之前节点必须需存在唯一用户任务节点，如果出现多个任务节点，则并行网关节点默认为结束节点，原因为不考虑多对多情况
        if (targetIds.size() > 1 && currentIds.size() > 1) {
            throw new CheckedException("任务出现多对多情况，无法撤回");
        }

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置驳回意见
        currentTaskIds.forEach(item -> taskService.addComment(item, task.getProcessInstanceId(), FlowComment.REJECT.getType(), flowTaskVo.getComment()));

        try {
            // 如果父级任务多于 1 个，说明当前节点不是并行节点，原因为不考虑多对多情况
            if (targetIds.size() > 1) {
                // 1 对 多任务跳转，currentIds 当前节点(1)，targetIds 跳转到的节点(多)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId()).
                        moveSingleActivityIdToActivityIds(currentIds.get(0), targetIds).changeState();
            }
            // 如果父级任务只有一个，因此当前任务可能为网关中的任务
            if (targetIds.size() == 1) {
                // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetIds.get(0) 跳转到的节点(1)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId())
                        .moveActivityIdsToSingleActivityId(currentIds, targetIds.get(0)).changeState();
            }
        } catch (FlowableObjectNotFoundException e) {
            throw new CheckedException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new CheckedException("无法取消或开始活动");
        }
    }

    /**
     * 退回任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> taskReturn(FlowTaskVo flowTaskVo) {
        if (taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult().isSuspended()) {
            throw new CheckedException("任务处于挂起状态");
        }
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        // 获取跳转的节点元素
        FlowElement target = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 当前任务节点元素
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = flowElement;
                }
                // 跳转的节点元素
                if (flowElement.getId().equals(flowTaskVo.getTargetKey())) {
                    target = flowElement;
                }
            }
        }

        // 从当前节点向前扫描
        // 如果存在路线上不存在目标节点，说明目标节点是在网关上或非同一路线上，不可跳转
        // 否则目标节点相对于当前节点，属于串行
        Boolean isSequential = FlowableUtils.iteratorCheckSequentialReferTarget(source, flowTaskVo.getTargetKey(), null, null);
        if (!isSequential) {
            throw new CheckedException("当前节点相对于目标节点，不属于串行关系，无法回退");
        }


        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需退回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置回退意见
        currentTaskIds.forEach(currentTaskId -> {
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), FlowComment.REBACK.getType(), flowTaskVo.getComment());
            Long userId = SecurityUtils.getLoginUser().getSysUser().getUserId();
            taskService.setAssignee(currentTaskId, userId.toString());
            taskService.setVariable(currentTaskId, "rejectOperator", flowTaskVo.getTargetKey());
        });

        try {
            // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetKey 跳转到的节点(1)
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentIds, flowTaskVo.getTargetKey()).changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new CheckedException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new CheckedException("无法取消或开始活动");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("rejectTaskKey", "");
        map.put("processStatus", "");
        return map;
    }


    /**
     * 获取所有可回退的节点
     *
     * @param flowTaskVo 参数
     */
    @Override
    public List<UserTask> findReturnTaskList(FlowTaskVo flowTaskVo) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息，暂不考虑子流程情况
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();
        // 获取当前任务节点元素
        UserTask source = null;
        if (flowElements != null) {
            for (FlowElement flowElement : flowElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = (UserTask) flowElement;
                }
            }
        }
        // 获取节点的所有路线
        List<List<UserTask>> roads = FlowableUtils.findRoad(source, null, null, null);
        // 可回退的节点列表
        List<UserTask> userTaskList = new ArrayList<>();
        for (List<UserTask> road : roads) {
            if (userTaskList.isEmpty()) {
                // 还没有可回退节点直接添加
                userTaskList = road;
            } else {
                // 如果已有回退节点，则比对取交集部分
                userTaskList.retainAll(road);
            }
        }
        return userTaskList;
    }

    /**
     * 删除任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    public void deleteTask(FlowTaskVo flowTaskVo) {
        taskService.deleteTask(flowTaskVo.getTaskId(), flowTaskVo.getComment());
    }

    /**
     * 认领/签收任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(FlowTaskVo flowTaskVo) {
        taskService.claim(flowTaskVo.getTaskId(), flowTaskVo.getUserId());
    }

    /**
     * 取消认领/签收任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unClaim(FlowTaskVo flowTaskVo) {
        taskService.unclaim(flowTaskVo.getTaskId());
    }

    /**
     * 委派任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(FlowTaskVo flowTaskVo) {
        taskService.delegateTask(flowTaskVo.getTaskId(), flowTaskVo.getAssignee());
    }


    /**
     * 转办任务
     *
     * @param flowTaskVo 请求实体参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(FlowTaskVo flowTaskVo) {
        if (ObjectUtils.allNull(flowTaskVo.getValues().get(ProcessConstants.PROCESS_APPROVAL))) {
            throw new CheckedException("未指定转办人");
        }
        if (String.valueOf(flowTaskVo.getValues().get(ProcessConstants.PROCESS_APPROVAL)).contains(",")) {
            throw new CheckedException("只能转办给一人");
        }
        //添加审批意见
        taskService.addComment(flowTaskVo.getTaskId(), flowTaskVo.getInstanceId(), FlowComment.NORMAL.getType(), flowTaskVo.getComment());
        taskService.setAssignee(flowTaskVo.getTaskId(), String.valueOf(flowTaskVo.getValues().get(ProcessConstants.PROCESS_APPROVAL)));
        //更新全局变量
        taskService.setVariables(flowTaskVo.getTaskId(), flowTaskVo.getVariables());
    }

    /**
     * 我发起的流程
     *
     * @param pageNum     当前页
     * @param pageSize    页大小
     * @param flowTaskDto 参数
     */
    @Override
    public Map<String, Object> myProcess(Integer pageNum, Integer pageSize, FlowTaskDto flowTaskDto) {
        Long userId = SecurityUtils.getLoginUser().getSysUser().getUserId();
        Map<String, Object> params = flowTaskDto.getParams();
        if (!SecurityUtils.isAdmin(userId)) {
            params.put("assignee", userId);
        }
        params.put("pageNum", pageSize * (pageNum - 1));
        params.put("pageSize", pageSize);
        flowTaskDto.setParams(params);
        Integer count = flowHistericTaskMapper.selectFlowHistoricProcessInstanceCount(flowTaskDto);
        List<HistoricProcessInstanceDTO> historicProcessInstances = flowHistericTaskMapper.selectFlowHistoricProcessInstance(flowTaskDto);
        List<FlowTaskDto> flowList = new ArrayList<>();
        for (HistoricProcessInstanceDTO hisIns : historicProcessInstances) {
            FlowTaskDto flowTask = new FlowTaskDto();
            flowTask.setCreateTime(hisIns.getStartTime());
            flowTask.setFinishTime(hisIns.getEndTime());
            flowTask.setProcInsId(hisIns.getProcInsId());
            flowTask.setProcessTitle(hisIns.getProcessTitle());
            flowTask.setFormId(hisIns.getFormId());
            // 计算耗时
            long time;
            if (Objects.nonNull(hisIns.getEndTime())) {
                time = hisIns.getEndTime().getTime() - hisIns.getStartTime().getTime();
            } else {
                time = System.currentTimeMillis() - hisIns.getStartTime().getTime();
            }
            flowTask.setDuration(DateUtils.getProcessCompletionTime(time));
            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(hisIns.getProcDefId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setCategory(pd.getCategory());
            flowTask.setProcDefVersion(pd.getVersion());
            // 当前所处流程
            R<SysUser> startUser = remoteuserservice.selectUserInFoById(hisIns.getStartUserId(), SecurityConstants.INNER);
            if (startUser.getCode() == HttpStatus.ERROR) {
                throw new CheckedException("获取用户信息失败");
            }
            flowTask.setStartUserId(String.valueOf(startUser.getData().getUserId()));
            flowTask.setStartUserName(startUser.getData().getNickName());
            flowTask.setStartDeptName(startUser.getData().getDept().getDeptName());
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(hisIns.getProcInsId()).orderByTaskCreateTime().desc().list();
            if (CollectionUtils.isNotEmpty(taskList)) {
                Task currtask = taskList.get(0);
                flowTask.setTaskId(currtask.getId());
                flowTask.setTaskName(currtask.getName());
                if (currtask.getAssignee() != null) {
                    R<SysUser> assigneeName = remoteuserservice.selectUserInFoById(Long.parseLong(currtask.getAssignee()), SecurityConstants.INNER);
                    if (assigneeName.getCode() == HttpStatus.ERROR) {
                        throw new CheckedException("获取用户信息失败");
                    }
                    flowTask.setAssigneeName(assigneeName.getData().getNickName());
                }

            } else {
                List<HistoricTaskInstance> historicTaskInstance = historyService.createHistoricTaskInstanceQuery().
                        processInstanceId(hisIns.getProcInsId()).orderByHistoricTaskInstanceEndTime().desc().list();
                if (CollectionUtils.isNotEmpty(historicTaskInstance)) {
                    HistoricTaskInstance histask = historicTaskInstance.get(0);
                    flowTask.setTaskId(histask.getId());
                    flowTask.setTaskName(histask.getName());
                    if (histask.getAssignee() != null) {
                        R<SysUser> assigneeName = remoteuserservice.selectUserInFoById(Long.parseLong(histask.getAssignee()), SecurityConstants.INNER);
                        if (assigneeName.getCode() == HttpStatus.ERROR) {
                            throw new CheckedException("获取用户信息失败");
                        }
                        flowTask.setAssigneeName(assigneeName.getData().getNickName());
                    }
                }
            }
            flowList.add(flowTask);
        }
        Map<String, Object> re = new HashMap<>(2);
        re.put("data", flowList);
        re.put("total", count);
        return re;
    }

    /**
     * 取消申请
     * 目前实现方式: 直接将当前流程变更为已完成
     *
     * @param flowTaskVo 参数
     */
    @Override
    public boolean stopProcess(FlowTaskVo flowTaskVo) {
        List<Task> task = taskService.createTaskQuery().processInstanceId(flowTaskVo.getInstanceId()).list();
        if (CollectionUtils.isEmpty(task)) {
            throw new CheckedException("流程未启动或已执行完成，取消申请失败");
        }
        // 获取当前需撤回的流程实例
        ProcessInstance processInstance =
                runtimeService.createProcessInstanceQuery()
                        .processInstanceId(flowTaskVo.getInstanceId())
                        .singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        if (Objects.nonNull(bpmnModel)) {
            Process process = bpmnModel.getMainProcess();
            List<EndEvent> endNodes = process.findFlowElementsOfType(EndEvent.class, false);
            if (CollectionUtils.isNotEmpty(endNodes)) {
                SysUser loginUser = SecurityUtils.getLoginUser().getSysUser();
                Authentication.setAuthenticatedUserId(loginUser.getUserId().toString());
                // 获取当前流程最后一个节点
                String endId = endNodes.get(0).getId();
                List<Execution> executions =
                        runtimeService.createExecutionQuery().parentId(processInstance.getProcessInstanceId()).list();
                List<String> executionIds = new ArrayList<>();
                executions.forEach(execution -> executionIds.add(execution.getId()));
                // 变更流程为已结束状态
                runtimeService.createChangeActivityStateBuilder()
                        .moveExecutionsToSingleActivityId(executionIds, endId).changeState();
            }
        }

        return true;
    }

    /**
     * 撤回任务到上一步
     *
     * @param flowTaskVo 参数
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revokeProcess(FlowTaskVo flowTaskVo) {
        String processInstanceId = flowTaskVo.getInstanceId();
        boolean processId = canRollback(processInstanceId, SecurityUtils.getUserId() + "", null);
        if (!processId) {
            throw new CheckedException("无法进行撤回操作");
        }
        // 获取当前流程实例的任务列表
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();
        if (tasks != null) {
            runtimeService.deleteProcessInstance(processInstanceId, "用户手动终止");
            historyService.deleteHistoricProcessInstance(processInstanceId);
        } else {
            throw new CheckedException("无法撤回");
        }

//        if (tasks.size() < 2) {
//            throw new CheckedException("无法撤回到上一步");
//        }
//
//        // 获取上一步任务
//        Task previousTask = tasks.get(1);
//        String previousTaskDefinitionKey = previousTask.getTaskDefinitionKey();
//
//        // 获取上一步任务的候选人员和候选组
//        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(previousTask.getId());
//        List<String> candidateUsers = new ArrayList<>();
//        List<String> candidateGroups = new ArrayList<>();
//
//        for (IdentityLink identityLink : identityLinks) {
//            if (identityLink.getUserId() != null) {
//                candidateUsers.add(identityLink.getUserId());
//            }
//            if (identityLink.getGroupId() != null) {
//                candidateGroups.add(identityLink.getGroupId());
//            }
//        }
//
//        // 检查当前用户是否属于候选人员或候选组
//        String currentUser = SecurityUtils.getUsername();
//        if (!candidateUsers.contains(currentUser) && !isUserInAnyGroup(currentUser, candidateGroups)) {
//            throw new CheckedException("您无权撤回到上一步");
//        }
//
//        // 执行撤回操作
//        runtimeService.createChangeActivityStateBuilder()
//                .processInstanceId(processInstanceId)
//                .moveActivityIdTo(previousTask.getTaskDefinitionKey(), previousTaskDefinitionKey)
//                .changeState();
        Map<String, Object> map = new HashMap<>();
        map.put("processStatus", "3");
        return map;
    }

    // 检查用户是否属于任何候选组
    private boolean isUserInAnyGroup(String user, List<String> candidateGroups) {
        List<Group> groups = identityService.createGroupQuery().groupMember(user).list();
        for (Group group : groups) {
            if (candidateGroups.contains(group.getId())) {
                return true;
            }
        }
        return false;
    }


    /**
     * 待办任务列表
     *
     * @param pageNum     当前页码
     * @param pageSize    每页条数
     * @param flowtaskdto 参数
     */
    @Override
    public Map<String, Object> todoList(Integer pageNum, Integer pageSize, FlowTaskDto flowtaskdto) {
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        List<String> roleList = new ArrayList<>();
        TaskQuery taskQuery = taskService.createTaskQuery();

        taskQuery
                .active()
                .includeProcessVariables();
        if (StringUtils.isNotBlank(flowtaskdto.getProcessTitle())) {
            List<SysProcessTitle> sysProcessTitle = flowDeployMapper.selectSysProcessTitleByProcessTitle(flowtaskdto.getProcessTitle());
            List<String> collect = sysProcessTitle.stream().map(SysProcessTitle::getProcInsId).collect(Collectors.toList());
            if (collect != null && !collect.isEmpty()) {
                taskQuery.processInstanceIdIn(collect);
            } else {
                taskQuery.processInstanceId("-1");
            }

        }
        if (ObjectUtils.isNotEmpty(flowtaskdto.getParams().get("beginTime"))) {
            taskQuery.taskCreatedAfter(DateUtils.parseDate(flowtaskdto.getParams().get("beginTime")));
        }
        if (ObjectUtils.isNotEmpty(flowtaskdto.getParams().get("endTime"))) {
            taskQuery.taskCreatedBefore(DateUtils.parseDate(flowtaskdto.getParams().get("endTime")));
        }
        taskQuery.or().taskAssignee(String.valueOf(sysUser.getUserId()))
                .taskCandidateOrAssigned(String.valueOf(sysUser.getUserId()));
        List<SysRole> roles = sysUser.getRoles();
        if (CollectionUtil.isNotEmpty(roles)) {
            roles.forEach(f ->
                    roleList.add(String.valueOf(f.getRoleId()))
            );
            taskQuery.taskCandidateGroupIn(roleList).endOr();
        } else {
            taskQuery.endOr();
        }
        Map<String, Map<String, Object>> map = new HashMap<>();
        List<Task> taskList = taskQuery.orderByTaskCreateTime().desc().listPage(pageSize * (pageNum - 1), pageSize);
        for (Task task : taskList) {
            Map<String, Object> variables = taskService.getVariables(task.getId());
            // 获取当前任务的流程实例ID
            String processInstanceId = task.getProcessInstanceId();
            // 查询历史任务列表，按结束时间降序排列
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .list();

            // 过滤当前任务并获取上一审批人
            String previousApprover = null;
            for (HistoricTaskInstance historicTask : historicTasks) {
                if (!historicTask.getId().equals(task.getId())) {
                    previousApprover = historicTask.getAssignee();
                    break;
                }
            }
            if (!StringUtils.isEmpty(previousApprover)) {
                R<SysUser> startUser = remoteuserservice.selectUserInFoById(Long.parseLong(previousApprover), SecurityConstants.INNER);
                if (startUser != null) {
                    variables.put("previousApprover", startUser.getData().getNickName());
                }
            }
            map.put(task.getId(), variables);
        }
        Map<String, Object> re = new HashMap<>(2);
        List<FlowTaskDto> flowTaskDtos = todoListIntegration(taskList);
        flowTaskDtos.stream().forEach(f -> {
            if (f.getTaskId() != null) {
                Map<String, Object> variables = map.get(f.getTaskId());
                String value = variables.get("detailUrl") + "";
                f.setDetailUrl(value);
                String businessContent = variables.get("businessContent") == null ? "" : variables.get("businessContent") + "";
                f.setBusinessContent(businessContent);
                String projectCode = variables.get("projectCode") == null ? "" : variables.get("projectCode") + "";
                f.setProjectCode(projectCode);
                if (variables.get("userObj") != null) {
                    JSONObject userObj = JSONObject.parseObject(variables.get("userObj") + "");
                    String businessId = userObj.get("businessId") == null ? "" : userObj.get("businessId") + "";
                    f.setBusinessId(businessId);
                    String businessType = userObj.get("businessType") == null ? "" : userObj.get("businessType") + "";
                    f.setBusinessType(businessType);
                }
                f.setPreviousApprover(variables.get("previousApprover") == null ? "" : variables.get("previousApprover") + "");
                f.setMessageStatus("未处理");
            }
        });

        re.put("data", flowTaskDtos);
        re.put("total", (int) taskQuery.count());
        return re;
    }

    /**
     * 待办任务列表
     *
     * @param pageNum     当前页码
     * @param pageSize    每页条数
     * @param flowtaskdto 参数
     */
    @Override
    public Map<String, Object> todoListV2(Integer pageNum, Integer pageSize, FlowTaskDto flowtaskdto) {
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        List<String> roleList = new ArrayList<>();
        TaskQuery taskQuery = taskService.createTaskQuery();

        taskQuery
                .active()
                .includeProcessVariables();
        if (StringUtils.isNotBlank(flowtaskdto.getProcDefName())) {
            taskQuery.processDefinitionNameLike(flowtaskdto.getProcDefName());
        }
        if (ObjectUtils.isNotEmpty(flowtaskdto.getParams().get("beginTime"))) {
            taskQuery.taskCreatedAfter(DateUtils.parseDate(flowtaskdto.getParams().get("beginTime")));
        }
        if (ObjectUtils.isNotEmpty(flowtaskdto.getParams().get("endTime"))) {
            taskQuery.taskCreatedBefore(DateUtils.parseDate(flowtaskdto.getParams().get("endTime")));
        }
//        taskQuery.or().taskAssignee(String.valueOf(sysUser.getUserId()))
//                .taskCandidateOrAssigned(String.valueOf(sysUser.getUserId()));
//        if(!sysUser.getRoles().isEmpty()){
//            sysUser.getRoles().forEach(f->
//                    roleList.add(String.valueOf(f.getRoleId()))
//            );
//            taskQuery.taskCandidateGroupIn(roleList).endOr();
//        }else {
//            taskQuery.endOr();
//        }
        List<Task> taskList = taskQuery.orderByTaskCreateTime().desc().listPage(pageSize * (pageNum - 1), pageSize);
        Map<String, Object> re = new HashMap<>(2);
        re.put("data", todoListIntegration(taskList));
        re.put("total", (int) taskQuery.count());
        return re;
    }

    /**
     * 待办任务列表整合
     */
    private List<FlowTaskDto> todoListIntegration(List<Task> taskList) {
        List<FlowTaskDto> flowList = new ArrayList<>();
        for (Task task : Objects.requireNonNull(taskList)) {
            FlowTaskDto flowTask = new FlowTaskDto();
            // 当前流程信息
            flowTask.setTaskId(task.getId());
            flowTask.setTaskDefKey(task.getTaskDefinitionKey());
            flowTask.setCreateTime(task.getCreateTime());
            flowTask.setProcDefId(task.getProcessDefinitionId());
            flowTask.setExecutionId(task.getExecutionId());
            flowTask.setTaskName(task.getName());
            //获取流程标题
            SysProcessTitle pt = flowDeployMapper.selectSysProcessTitle(task.getProcessInstanceId());
            if (pt != null) {
                flowTask.setProcessTitle(pt.getProcessTitle());
            }
            // 流程定义信息
            FlowProcDefDto pd = flowDeployMapper.selectActReProcDef(task.getProcessDefinitionId());
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(task.getProcessInstanceId());
            flowTask.setFormId(pd.getFormId());
            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (historicProcessInstance != null && StringUtils.isNotBlank(historicProcessInstance.getStartUserId())) {
                R<SysUser> startUser = remoteuserservice.selectUserInFoById(Long.parseLong(historicProcessInstance.getStartUserId()), SecurityConstants.INNER);
                if (startUser.getCode() == HttpStatus.ERROR) {
                    throw new CheckedException("获取用户信息失败:" + startUser.getMsg());
                }
                flowTask.setStartUserId(String.valueOf(startUser.getData().getUserId()));
                flowTask.setStartUserName(startUser.getData().getNickName());
                SysDept dept = startUser.getData().getDept();
                String deptName = ObjectUtil.isEmpty(dept) ? null : dept.getDeptName();
                flowTask.setStartDeptName(deptName);
                flowList.add(flowTask);
            }
        }
        return flowList;
    }

    /**
     * 已办任务列表
     *
     * @param pageNum     当前页码
     * @param pageSize    每页条数
     * @param flowTaskDto 参数
     */
    @Override
    public Map<String, Object> finishedList(Integer pageNum, Integer pageSize, FlowTaskDto flowTaskDto) {
        Map<String, Object> params = flowTaskDto.getParams();
        params.put("assignee", SecurityUtils.getLoginUser().getSysUser().getUserId());
        params.put("pageNum", pageSize * (pageNum - 1));
        params.put("pageSize", pageSize);
        flowTaskDto.setParams(params);

        List<HistoricTaskInstanceDTO> historicTaskInstanceList = flowHistericTaskMapper.selectFlowHistericTaskInstance(flowTaskDto);
        Integer hcount = flowHistericTaskMapper.selectFlowHistericTaskInstanceCount(flowTaskDto);
        List<FlowTaskDto> hisTaskList = Lists.newArrayList();
        for (HistoricTaskInstanceDTO inst : historicTaskInstanceList) {
            AtomicReference<FlowTaskDto> flowTasks = new AtomicReference<>(new FlowTaskDto());
            flowTasks.get().setTaskId(inst.getIdRev());
            flowTasks.get().setCreateTime(inst.getStartTime());
            flowTasks.get().setFinishTime(inst.getEndTime());
            flowTasks.get().setDuration(DateUtils.getProcessCompletionTime(inst.getDuration()));
            flowTasks.get().setProcDefId(inst.getProcDefId());
            flowTasks.get().setTaskDefKey(inst.getTaskDefKey());
            flowTasks.get().setTaskName(inst.getNname());
            flowTasks.get().setExecutionId(inst.getExecutionId());
            flowTasks.get().setProcessTitle(inst.getProcessTitle());
            flowTasks.get().setStartDeptName(inst.getStartDeptName());
            flowTasks.get().setStartUserId(inst.getStartUserId());
            flowTasks.get().setStartUserName(inst.getStartUserName());

            // 流程定义信息
            FlowProcDefDto pd = flowDeployMapper.selectActReProcDef(inst.getProcDefId());
            flowTasks.get().setDeployId(pd.getDeploymentId());
            flowTasks.get().setProcDefName(pd.getName());
            flowTasks.get().setProcDefVersion(pd.getVersion());
            flowTasks.get().setProcInsId(inst.getProcInstId());
            flowTasks.get().setHisProcInsId(inst.getProcInstId());
            flowTasks.get().setFormId(pd.getFormId());


            List<HistoricVariableInstance> historicVars = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(inst.getProcInstId()).list();
            Map<String, Object> variables = new HashMap<>();
            historicVars.stream().forEach(historicVar -> {
                variables.put(historicVar.getVariableName(), historicVar.getValue());
            });


            // 获取当前任务的流程实例ID
            String processInstanceId = inst.getProcInstId();
            // 查询历史任务列表，按结束时间降序排列
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .list();

            // 过滤当前任务并获取上一审批人
            String previousApprover = null;
            for (HistoricTaskInstance historicTask : historicTasks) {
                previousApprover = historicTask.getAssignee();
                break;
            }
            if (!StringUtils.isEmpty(previousApprover)) {
                R<SysUser> startUser = remoteuserservice.selectUserInFoById(Long.parseLong(previousApprover), SecurityConstants.INNER);
                if (startUser != null) {
                    variables.put("previousApprover", startUser.getData().getNickName());
                }
            }
            String value = variables.get("detailUrl") + "";
            flowTasks.get().setDetailUrl(value);
            String businessContent = variables.get("businessContent") == null ? "" : variables.get("businessContent") + "";
            flowTasks.get().setBusinessContent(businessContent);
            String projectCode = variables.get("projectCode") == null ? "" : variables.get("projectCode") + "";
            flowTasks.get().setProjectCode(projectCode);
            if (variables.get("userObj") != null) {
                JSONObject userObj = JSONObject.parseObject(variables.get("userObj") + "");
                String businessId = userObj.get("businessId") == null ? "" : userObj.get("businessId") + "";
                flowTasks.get().setBusinessId(businessId);
                String businessType = userObj.get("businessType") == null ? "" : userObj.get("businessType") + "";
                flowTasks.get().setBusinessType(businessType);
            }
            flowTasks.get().setPreviousApprover(variables.get("previousApprover") == null ? "" : variables.get("previousApprover") + "");
            flowTasks.get().setMessageStatus("已处理");

            hisTaskList.add(flowTasks.get());
        }
        Map<String, Object> re = new HashMap<>(2);
        re.put("data", hisTaskList);
        re.put("total", hcount);
        return re;
    }

    /**
     * 已办任务列表
     *
     * @param pageNum     当前页码
     * @param pageSize    每页条数
     * @param flowTaskDto 参数
     */
    @Override
    public Map<String, Object> finishedListV2(Integer pageNum, Integer pageSize, FlowTaskDto flowTaskDto) {
        Map<String, Object> params = flowTaskDto.getParams();
        params.put("assignee", SecurityUtils.getLoginUser().getSysUser().getUserId());
        params.put("pageNum", pageSize * (pageNum - 1));
        params.put("pageSize", pageSize);
        flowTaskDto.setParams(params);

        List<HistoricTaskInstanceDTO> historicTaskInstanceList = flowHistericTaskMapper.selectFlowHistericTaskInstanceV2(flowTaskDto);
        Integer hcount = flowHistericTaskMapper.selectFlowHistericTaskInstanceCount(flowTaskDto);
        List<FlowTaskDto> hisTaskList = Lists.newArrayList();
        for (HistoricTaskInstanceDTO inst : historicTaskInstanceList) {
            AtomicReference<FlowTaskDto> flowTasks = new AtomicReference<>(new FlowTaskDto());
            flowTasks.get().setTaskId(inst.getIdRev());
            flowTasks.get().setCreateTime(inst.getStartTime());
            flowTasks.get().setFinishTime(inst.getEndTime());
            flowTasks.get().setDuration(DateUtils.getProcessCompletionTime(inst.getDuration()));
            flowTasks.get().setProcDefId(inst.getProcDefId());
            flowTasks.get().setTaskDefKey(inst.getTaskDefKey());
            flowTasks.get().setTaskName(inst.getNname());
            flowTasks.get().setExecutionId(inst.getExecutionId());
            flowTasks.get().setProcessTitle(inst.getProcessTitle());
            flowTasks.get().setStartDeptName(inst.getStartDeptName());
            flowTasks.get().setStartUserId(inst.getStartUserId());
            flowTasks.get().setStartUserName(inst.getStartUserName());

            // 流程定义信息
            FlowProcDefDto pd = flowDeployMapper.selectActReProcDef(inst.getProcDefId());
            flowTasks.get().setDeployId(pd.getDeploymentId());
            flowTasks.get().setProcDefName(pd.getName());
            flowTasks.get().setProcDefVersion(pd.getVersion());
            flowTasks.get().setProcInsId(inst.getProcInstId());
            flowTasks.get().setHisProcInsId(inst.getProcInstId());
            flowTasks.get().setFormId(pd.getFormId());
            hisTaskList.add(flowTasks.get());
        }
        Map<String, Object> re = new HashMap<>(2);
        re.put("data", hisTaskList);
        re.put("total", hcount);
        return re;
    }

    /**
     * 抄送列表
     *
     * @param pageNum     当前页码
     * @param pageSize    每页条数
     * @param flowTaskDto 参数
     */
    @Override
    public Map<String, Object> ccList(Integer pageNum, Integer pageSize, FlowTaskDto flowTaskDto) {
        Map<String, Object> params = flowTaskDto.getParams();
        params.put("userId", SecurityUtils.getLoginUser().getSysUser().getUserId());
        params.put("pageNum", pageSize * (pageNum - 1));
        params.put("pageSize", pageSize);
        flowTaskDto.setParams(params);
        List<CourtesyCopyDTO> copyDTOList = sysTaskCcMapper.selectSysTaskCcList(flowTaskDto);
        List<FlowTaskDto> ccList = Lists.newArrayList();
        if (!copyDTOList.isEmpty()) {
            for (CourtesyCopyDTO inst : copyDTOList) {
                AtomicReference<FlowTaskDto> flowTasks = new AtomicReference<>(new FlowTaskDto());
                flowTasks.get().setTaskId(inst.getTaskId());
                flowTasks.get().setCreateTime(inst.getStartTime());
                flowTasks.get().setProcDefId(inst.getProcDefId());
                flowTasks.get().setTaskName(inst.getNname());
                flowTasks.get().setExecutionId(inst.getExecutionId());
                flowTasks.get().setProcessTitle(inst.getProcessTitle());
                flowTasks.get().setStartDeptName(inst.getStartDeptName());
                flowTasks.get().setStartUserId(inst.getStartUserId());
                flowTasks.get().setStartUserName(inst.getStartUserName());
                // 流程定义信息
                FlowProcDefDto pd = flowDeployMapper.selectActReProcDef(inst.getProcDefId());
                flowTasks.get().setDeployId(pd.getDeploymentId());
                flowTasks.get().setProcDefName(pd.getName());
                flowTasks.get().setProcDefVersion(pd.getVersion());
                flowTasks.get().setProcInsId(inst.getProcInstId());
                flowTasks.get().setFormId(pd.getFormId());
                ccList.add(flowTasks.get());
            }
        }

        Map<String, Object> re = new HashMap<>(2);
        re.put("data", ccList);
        re.put("total", copyDTOList.size());
        return re;
    }

    /**
     * 流程历史流转记录
     *
     * @param procInsId 流程实例ID
     * @param deployId  部署id
     */
    @Override
    public Map<String, Object> flowRecord(String procInsId, String deployId) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(procInsId)) {
            List<HistoricActivityInstance> list = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(procInsId)
                    .orderByHistoricActivityInstanceEndTime()
                    .desc().list();

            String startUserId;
            // 获取当前的流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(procInsId).singleResult();
            // 如果流程已经结束，则得到结束节点
            if (Objects.isNull(processInstance)) {
                HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(procInsId).singleResult();
                startUserId = pi.getStartUserId();
            } else {// 如果流程没有结束，则取当前活动节点
                // 根据流程实例ID获得当前处于活动状态的ActivityId合集
                ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInsId).singleResult();
                startUserId = pi.getStartUserId();
            }


            List<FlowTaskDto> hisFlowList = new ArrayList<>();
            for (HistoricActivityInstance histIns : list) {
                if (StringUtils.isNotBlank(histIns.getTaskId())) {
                    FlowTaskDto flowTask = new FlowTaskDto();
                    flowTask.setTaskId(histIns.getTaskId());
                    flowTask.setTaskName(histIns.getActivityName());
                    flowTask.setCreateTime(histIns.getStartTime());
                    flowTask.setFinishTime(histIns.getEndTime());
                    if (StringUtils.isNotBlank(histIns.getAssignee())) {
                        SysUser sysUser = remoteuserservice.selectUserInFoById(Long.parseLong(histIns.getAssignee()), SecurityConstants.INNER).getData();
                        flowTask.setAssigneeId(sysUser.getUserId());
                        flowTask.setAssigneeName(sysUser.getNickName());
                        if (sysUser.getDept() != null) {
                            flowTask.setDeptName(sysUser.getDept().getDeptName());
                        }
                    }
                    // 展示审批人员
                    List<HistoricIdentityLink> linksForTask = historyService.getHistoricIdentityLinksForTask(histIns.getTaskId());
                    StringBuilder stringBuilder = new StringBuilder();
                    for (HistoricIdentityLink identityLink : linksForTask) {
                        // 获选人,候选组/角色(多个)
                        if ("candidate".equals(identityLink.getType())) {
                            if (StringUtils.isNotBlank(identityLink.getUserId())) {
                                R<SysUser> sysUserR = remoteuserservice.selectUserInFoById(Long.parseLong(identityLink.getUserId()), SecurityConstants.INNER);
                                if (sysUserR.getCode() == HttpStatus.ERROR) {
                                    throw new CheckedException("获取用户信息失败");
                                }
                                stringBuilder.append(sysUserR.getData().getNickName()).append(",");
                            }
                            if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                                R<SysRole> sysRoleR = remoteuserservice.selectRoleById(Long.parseLong(identityLink.getGroupId()), SecurityConstants.INNER);
                                if (sysRoleR.getCode() == HttpStatus.ERROR) {
                                    throw new CheckedException("获取角色信息失败");
                                }
                                stringBuilder.append(sysRoleR.getData().getRoleName()).append(",");
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(stringBuilder)) {
                        flowTask.setCandidate(stringBuilder.substring(0, stringBuilder.length() - 1));
                    }
                    flowTask.setDuration(histIns.getDurationInMillis() == null || histIns.getDurationInMillis() == 0 ? null : DateUtils.getProcessCompletionTime(histIns.getDurationInMillis()));
                    // 获取意见评论内容
                    List<Comment> commentList = taskService.getProcessInstanceComments(histIns.getProcessInstanceId());
                    StringBuilder stl = new StringBuilder();
                    commentList.forEach(comment -> {
                        if (histIns.getTaskId().equals(comment.getTaskId())) {
                            if (comment.getFullMessage() != null) {
                                stl.append(comment.getFullMessage()).append("; ");
                                flowTask.setCategory(comment.getType());
                                if (FlowComment.INITIATE.getType().equals(comment.getType())) {
                                    SysUser sysUser = remoteuserservice.selectUserInFoById(Long.parseLong(startUserId), SecurityConstants.INNER).getData();
                                    flowTask.setAssigneeId(sysUser.getUserId());
                                    flowTask.setAssigneeName(sysUser.getNickName());
                                }
                            }
                        }
                        flowTask.setComment(FlowCommentDto.builder().type(comment.getType()).comment(String.valueOf(stl)).build());
                    });
//                    if (!StringUtils.isEmpty(flowTask.getAssigneeName())) {
                    hisFlowList.add(flowTask);
//                    }
//                    hisFlowList.add(flowTask);
                }
            }
            map.put("flowList", hisFlowList);
        }
        // 如果是第一次流程发起，获取初始化配置的表单
        if (StringUtils.isNotBlank(deployId)) {
            SysForm sysForm = sysInstanceFormService.selectSysDeployFormByDeployId(deployId);
            if (sysForm != null) {
                map.put("formData", JSONObject.parseObject(sysForm.getFormContent()));
            }
        }
        return map;
    }


    /**
     * 获取流程所有审批节点信息
     *
     * @param processId 流程实例ID
     * @return 节点信息列表
     */
    public List<Map<String, Object>> loadTaskDef(String processId) {
        List<Map<String, Object>> result = new ArrayList<>();


        String processDefinitionId;
        String startUserId;
        // 获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        // 如果流程已经结束，则得到结束节点
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
            startUserId = pi.getStartUserId();
        } else {// 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
            startUserId = pi.getStartUserId();
        }

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedFlowList = historyService.
                createHistoricActivityInstanceQuery().
                processInstanceId(processId).
                orderByHistoricActivityInstanceStartTime()
                .asc().list();

        Map<String, Boolean> map = new HashMap<>();
        Map<String, String> usMap = new HashMap<>();
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if (StringUtils.isNotBlank(tempActivity.getTaskId())) {
                map.put(tempActivity.getActivityId(), !Objects.isNull(tempActivity.getEndTime()));
                usMap.put(tempActivity.getActivityId(), tempActivity.getAssignee());
            }
        }


        // 2. 获取流程定义模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        // Process process = bpmnModel.getMainProcess();

        // Collection<FlowElement> flowElements = process.getFlowElements();
        // Collection<UserTask> tasks  = getAllUserTaskEvent(flowElements, null);
        // List<UserTask> userTasks = sortTasksByFlowPath(getStartEvent(bpmnModel), tasks);
        // 3. 获取所有用户任务节点
        //List<UserTask> userTasks = process.findFlowElementsOfType(UserTask.class);

        List<List<UserTask>> leveledTasks = sortLeveledTasks(bpmnModel);
        int i = 0;
        int level = 0;
        for (List<UserTask> taskGroup : leveledTasks) {

//        int i = 0;
        // 5. 处理每个任务节点
        for (UserTask userTask : taskGroup) {
            i++;
            Map<String, Object> nodeInfo = new LinkedHashMap<>();
            // 基础信息
            nodeInfo.put("taskId", userTask.getId());
            nodeInfo.put("taskName", userTask.getName());
            nodeInfo.put("nodeKey", userTask.getId());
            nodeInfo.put("nodeName", userTask.getName());
            nodeInfo.put("level", level);           // 新增：层级标识
            nodeInfo.put("parallel", taskGroup.size() > 1); // 新增：是否并行
            Boolean completed = false;
            if (map.get(userTask.getId()) != null) {
                completed = map.get(userTask.getId());
            }
            nodeInfo.put("completed", completed);
            ArrayList<Map<String, Object>> userList = new ArrayList<>();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("appoint", false);
            userMap.put("completed", completed);
            userMap.put("userId", usMap.get(userTask.getId()));
            if (!StringUtils.isEmpty(usMap.get(userTask.getId()))) {
                userMap.put("userName", remoteuserservice.getUserInfoById(Long.parseLong(usMap.get(userTask.getId())), SecurityConstants.INNER).getNickName());
            } else if (i == 1) {
                userMap.put("userName", remoteuserservice.getUserInfoById(Long.parseLong(startUserId), SecurityConstants.INNER).getNickName());
            } else {
                userMap.put("userName", userTask.getAssignee());
            }
            userList.add(userMap);
            nodeInfo.put("userList", userList);
            ArrayList<Map<String, Object>> postList = new ArrayList<>();
            if (userTask.getCandidateGroups() != null) {
                for (String str : userTask.getCandidateGroups()) {
                    R<SysRole> sysRoleR = remoteuserservice.selectRoleById(Long.parseLong(str), SecurityConstants.INNER);
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("corpOrg", completed);
                    postMap.put("postId", str);
                    postMap.put("orgName", sysRoleR.getData().getRoleName());
                    postList.add(postMap);
                }
            }
            nodeInfo.put("taskPost", postList);
            result.add(nodeInfo);
        }
        level++;
        }

        return result;
    }

    private static Collection<UserTask> getAllUserTaskEvent(Collection<FlowElement> flowElements, Collection<UserTask> allElements) {
        allElements = allElements == null ? new ArrayList<>() : allElements;
        for (FlowElement flowElement : flowElements) {

            if (flowElement instanceof UserTask) {
                allElements.add((UserTask) flowElement);
            }
            if (flowElement instanceof SubProcess) {
                // 继续深入子流程，进一步获取子流程
                allElements = getAllUserTaskEvent(((SubProcess) flowElement).getFlowElements(), allElements);
            }
        }
        return allElements;
    }

    private static StartEvent getStartEvent(BpmnModel model) {
        Process process = model.getMainProcess();
        FlowElement startElement = process.getInitialFlowElement();
        if (startElement instanceof StartEvent) {
            return (StartEvent) startElement;
        }
        return null;
    }

    private static List<List<UserTask>> sortLeveledTasks(BpmnModel bpmnModel) {
        List<List<UserTask>> levels = new ArrayList<>();
        StartEvent startEvent = getStartEvent(bpmnModel);
        if (startEvent == null) {
            throw new CheckedException("未找到开始节点");
        }
        
        // BFS 遍历，key: 节点id, value: level
        Map<String, Integer> levelMap = new HashMap<>();
        // 队列元素: [节点, level]
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[]{startEvent.getOutgoingFlows().get(0).getTargetFlowElement(), 0});
        
        while (!queue.isEmpty()) {
            Object[] pair = queue.poll();
            FlowElement element = (FlowElement) pair[0];
            int level = (int) pair[1];
            
            if (element == null) continue;
            if (element instanceof EndEvent) continue;
            
            if (element instanceof UserTask) {
                // 取最小 level（更先到达的路径为准）
                levelMap.merge(element.getId(), level, Math::min);
                // 从 UserTask 继续往后走，进入下一 level
                for (SequenceFlow flow : ((UserTask) element).getOutgoingFlows()) {
                    queue.add(new Object[]{flow.getTargetFlowElement(), level + 1});
                }
            } else if (element instanceof ParallelGateway) {
                // 并行网关：所有出口路径在同一 level
                for (SequenceFlow flow : ((ParallelGateway) element).getOutgoingFlows()) {
                    queue.add(new Object[]{flow.getTargetFlowElement(), level});
                }
            } else if (element instanceof ExclusiveGateway) {
                // 排他网关：走条件为 "pass" 的路径
                SequenceFlow passWay = ((ExclusiveGateway) element).getOutgoingFlows().stream()
                    .filter(flow -> flow.getConditionExpression() != null 
                        && flow.getConditionExpression().contains("pass"))
                    .findFirst()
                    .orElse(null);
                if (passWay != null) {
                    queue.add(new Object[]{passWay.getTargetFlowElement(), level});
                }
            } else if (element instanceof StartEvent) {
                // 从开始节点往后走
                for (SequenceFlow flow : ((StartEvent) element).getOutgoingFlows()) {
                    queue.add(new Object[]{flow.getTargetFlowElement(), level});
                }
            }
        }
        
        // 将 levelMap 转为 List<List<UserTask>>
        Map<Integer, List<UserTask>> levelGroup = new TreeMap<>();
        for (UserTask task : getAllUserTaskEvent(bpmnModel.getMainProcess().getFlowElements(), null)) {
            Integer lvl = levelMap.get(task.getId());
            if (lvl != null) {
                levelGroup.computeIfAbsent(lvl, k -> new ArrayList<>()).add(task);
            }
        }
        
        return new ArrayList<>(levelGroup.values());
    }

    private static List<UserTask> sortTasksByFlowPath(StartEvent startEvent, Collection<UserTask> userTasks) {
        List<UserTask> sorted = new ArrayList<>(userTasks.size());
        FlowElement next = startEvent.getOutgoingFlows().get(0).getTargetFlowElement();
        if (next == null) {
            throw new CheckedException("流程图开始节点未找到目标节点");
        }
        // 第一个节点
        sorted.add((UserTask) next);
        boolean end = false;
        while (!end) {
            next = findTargetNode(next);
            if (next instanceof Gateway) {
                continue;
            }
            if (next instanceof EndEvent) {
                end = true;
                continue;
            }
            if (next == null) {
                end = true;
            } else {
                sorted.add((UserTask) next);
            }
        }

        return sorted;
    }

    private static FlowElement findTargetNode(FlowElement node) {
        if (node instanceof Gateway) {
            SequenceFlow passWay = ((Gateway) node).getOutgoingFlows().stream()
                    .filter(flow -> flow.getConditionExpression().contains("pass"))
                    .findFirst()
                    .orElseThrow(()-> new CheckedException("网关未正确配置流转条件"));
            return passWay.getTargetFlowElement();
        }
        if (node instanceof EndEvent) {
            return null;
        }
        if (node == null) {
            return null;
        }
        final UserTask userTask = (UserTask) node;
        // 用户任务节点默认只支持一个出口路径
        return userTask.getOutgoingFlows().get(0).getTargetFlowElement();
    }

    /**
     * 获取流程过程图
     *
     * @param processId 参数
     */
    @Override
    public InputStream diagram(String processId) {
        String processDefinitionId;
        // 获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        // 如果流程已经结束，则得到结束节点
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();

            processDefinitionId = pi.getProcessDefinitionId();
        } else {// 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> highLightedFlows = new ArrayList<>();
        List<String> highLightedNodes = new ArrayList<>();
        //高亮线
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if ("sequenceFlow".equals(tempActivity.getActivityType())) {
                //高亮线
                highLightedFlows.add(tempActivity.getActivityId());
            } else {
                //高亮节点
                highLightedNodes.add(tempActivity.getActivityId());
            }
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        //获取自定义图片生成器
        ProcessDiagramGenerator diagramGenerator = new CustomProcessDiagramGenerator();
        return diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodes, highLightedFlows, configuration.getActivityFontName(),
                configuration.getLabelFontName(), configuration.getAnnotationFontName(), configuration.getClassLoader(), 1.0, true);

    }

    /**
     * 获取流程执行过程
     *
     * @param procInsId 流程实例id
     */
    @Override
    public List<FlowViewerDto> getFlowViewer(String procInsId, String executionId) {
        List<FlowViewerDto> flowViewerList = new ArrayList<>();
        FlowViewerDto flowViewerDto;
        // 获取任务开始节点(临时处理方式)
        List<HistoricActivityInstance> startNodeList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInsId)
                .orderByHistoricActivityInstanceStartTime()
                .asc().listPage(0, 100);
        for (HistoricActivityInstance startInstance : startNodeList) {
            if (!"sequenceFlow".equals(startInstance.getActivityType())) {
                flowViewerDto = new FlowViewerDto();
                if (!"sequenceFlow".equals(startInstance.getActivityType())) {
                    flowViewerDto.setKey(startInstance.getActivityId());
                    // 根据流程节点处理时间校验该节点是否已完成
                    flowViewerDto.setCompleted(!Objects.isNull(startInstance.getEndTime()));
                    flowViewerList.add(flowViewerDto);
                }
            }
        }
        // 历史节点
        List<HistoricActivityInstance> hisActIns = historyService.createHistoricActivityInstanceQuery()
                .executionId(executionId)
                .orderByHistoricActivityInstanceStartTime()
                .asc().list();
        for (HistoricActivityInstance activityInstance : hisActIns) {
            if (!"sequenceFlow".equals(activityInstance.getActivityType())) {
                flowViewerDto = new FlowViewerDto();
                flowViewerDto.setKey(activityInstance.getActivityId());
                // 根据流程节点处理时间校验该节点是否已完成
                flowViewerDto.setCompleted(!Objects.isNull(activityInstance.getEndTime()));
                flowViewerList.add(flowViewerDto);
            }
        }
        return flowViewerList;
    }

    /**
     * 获取流程变量
     *
     * @param taskId 任务ID
     */
    @Override
    public Map<String, Object> processVariables(String taskId) {
        // 流程变量
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().finished().taskId(taskId).singleResult();
        if (Objects.nonNull(historicTaskInstance)) {
            return historicTaskInstance.getProcessVariables();
        } else {
            return taskService.getVariables(taskId);
        }
    }

    /**
     * 获取下一节点
     *
     * @param flowTaskVo 任务
     * @return
     */
    @Override
    public FlowNextDto getNextFlowNodeByStart(FlowTaskVo flowTaskVo) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(flowTaskVo.getDeploymentId()).singleResult();
        // Step 1. 获取当前节点并找到下一步节点
        FlowNextDto flowNextDto = new FlowNextDto();
        // Step 2. 获取当前流程所有流程变量(网关节点时需要校验表达式)
        List<UserTask> nextUserTask = FindNextNodeUtil.getNextUserTasksByStart(repositoryService, processDefinition, flowTaskVo.getVariables());
        if (CollectionUtils.isNotEmpty(nextUserTask)) {
            for (UserTask userTask : nextUserTask) {
                MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                // 会签节点
                if (Objects.nonNull(multiInstance)) {
                    flowNextDto.setVars(multiInstance.getInputDataItem());
                    flowNextDto.setType(ProcessConstants.PROCESS_MULTI_INSTANCE);
                    flowNextDto.setDataType(ProcessConstants.DATA_TYPE);
                } else {
                    // 读取自定义节点属性 判断是否是否需要动态指定任务接收人员、组
                    String dataType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_DATA_TYPE);
                    String userType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_USER_TYPE);
                    flowNextDto.setVars(ProcessConstants.PROCESS_APPROVAL);
                    flowNextDto.setType(userType);
                    flowNextDto.setDataType(dataType);
                }
            }
        }
        return flowNextDto;
    }

    /**
     * 判断当前用户是否可以撤回流程
     *
     * @param processInstanceId 流程实例ID
     * @param currentUserId     当前用户ID
     * @param currentTaskId     当前任务ID（可为空，自动查询）
     * @return 是否允许撤回
     */
    public boolean canRollback(String processInstanceId, String currentUserId, String currentTaskId) {
        // 1. 验证流程实例是否存在
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            return false;
        }

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")  // 过滤用户任务节点
                .finished()  // 仅查询已完成的节点
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        //只能撤回刚刚提交的节点
        if (activities != null && !activities.isEmpty() && activities.size() > 1) {
            return false;
        }

        // 2. 校验当前用户为上报人
        String submitter = (String) runtimeService.getVariable(processInstanceId, "INITIATOR");
        if (!currentUserId.equals(submitter)) {
            return false;
        }
        return true;

    }

    private boolean checkUserPermission(String processInstanceId, String currentUserId, Task currentTask, HistoricTaskInstance historicTask) {
        // 验证是否为发起人
        String initiator = (String) runtimeService.getVariable(processInstanceId, "INITIATOR");
        if (currentUserId.equals(initiator)) return true;

        // 验证是否为任务办理人或候选用户
        if (currentTask != null) {
            return currentUserId.equals(currentTask.getAssignee()) || isCandidateUser(currentTask, currentUserId);
        } else if (historicTask != null) {
            return currentUserId.equals(historicTask.getAssignee()) || isCandidateUser(historicTask, currentUserId);
        }
        return false;
    }

    private boolean isCandidateUser(Task task, String userId) {
        TaskQuery query = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .taskId(task.getId());
        return query.count() > 0;
    }

    private boolean isCandidateUser(HistoricTaskInstance task, String userId) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateUser(userId)
                .taskId(task.getId());
        return query.count() > 0;
    }


    private boolean checkIfNextNodeExists(String processInstanceId, Task currentTask, HistoricTaskInstance historicTask) {
        Date checkTime = (currentTask != null) ? currentTask.getCreateTime() : historicTask.getEndTime();
        List<HistoricActivityInstance> nextActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .startedAfter(checkTime)
                .list();
        return !nextActivities.isEmpty();
    }


    @Override
    public Map<String, Object> initialize(Map<String, Object> variables) {
        Map<String, Object> variablesMap = new HashMap<>();
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(variables.get("processId") + "")
                .singleResult();
        if (instance == null) {
            variablesMap.put("revokable", false);
            variablesMap.put("auditable", false);
            variablesMap.put("businessId", null);
            variablesMap.put("processId", null);
            return variablesMap;
        }
        boolean processId = canRollback(variables.get("processId") + "", sysUser.getUserId() + "", null);
        variablesMap.put("revokable", processId);
        TaskService taskService = processEngine.getTaskService();

        List<Task> userTasks = taskService.createTaskQuery()
                .processInstanceId(variables.get("processId") + "")
                .taskAssignee(SecurityUtils.getUserId() + "")
                .active()
                .list();

        List<SysRole> roles = sysUser.getRoles();
        List<String> collect = roles.stream().map(SysRole::getRoleId).map(String::valueOf).collect(Collectors.toList());
        List<Task> userTasks1 = taskService.createTaskQuery()
                .processInstanceId(variables.get("processId") + "")
                .active()
                .taskCandidateGroupIn(collect)
                .list();
        String businessId = "";
        FlowTaskVo flowTaskVo = new FlowTaskVo();
        List<Map<String, Object>> maps = new ArrayList<>();
        if (!userTasks.isEmpty()) {
            businessId = this.processVariables(userTasks.get(0).getId()).get("businessId") + "";
            variablesMap.put("curTaskId", userTasks.get(0).getId());
            flowTaskVo.setTaskId(userTasks.get(0).getId());
            List<UserTask> returnTaskList = findReturnTaskList(flowTaskVo);
            returnTaskList.forEach(userTask -> {
                Map<String, Object> map = new HashMap<>();
                map.put("taskKey", userTask.getId());
                map.put("taskName", userTask.getName());
                maps.add(map);
            });
        }
        if (!userTasks1.isEmpty()) {
            businessId = this.processVariables(userTasks1.get(0).getId()).get("businessId") + "";
            variablesMap.put("curTaskId", userTasks1.get(0).getId());
            flowTaskVo.setTaskId(userTasks1.get(0).getId());
            List<UserTask> returnTaskList = findReturnTaskList(flowTaskVo);
            returnTaskList.forEach(userTask -> {
                Map<String, Object> map = new HashMap<>();
                map.put("taskKey", userTask.getId());
                map.put("taskName", userTask.getName());
                maps.add(map);
            });
        }
        variablesMap.put("completedTaskList", maps);
        variablesMap.put("auditable", !userTasks.isEmpty() || !userTasks1.isEmpty());
        variablesMap.put("businessId", businessId);
        variablesMap.put("processId", variables.get("processId"));
        variablesMap.put("nextAppointable", false);
        variablesMap.put("nextCandidateList", new ArrayList<>());


//        /* 下一步审批人列表 */
//        this.nextCandidateList = res.data.nextCandidateList;
//        /* 下一步审批人是否可选 */
//        this.nextAppointable = res.data.nextAppointable;
//        /* 任务阶段 */
//        this.taskPresentId = res.data.curTaskId;
//        /* 是否可以审批 */
//        this.isShowButton = res.data.auditable;


        return variablesMap;
    }

    /**
     * 获取当前任务的流程定义ID
     */
    private String getProcessDefinitionIdByTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        return instance.getProcessDefinitionId();
    }

    /**
     * 获取当前流程可驳回的节点列表
     *
     * @param taskId      当前任务ID
     * @param currentUser 当前用户
     * @return 可驳回节点列表（包含节点ID、名称、类型）
     */
    public List<Map<String, String>> getRevokableNodes(String taskId, String currentUser) {
        // 1. 验证当前任务和流程实例
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask == null) {
            throw new FlowableException("任务不存在或已结束");
        }

        String processInstanceId = currentTask.getProcessInstanceId();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            throw new FlowableException("流程实例已结束");
        }

//        // 2. 权限验证（发起人或当前处理人）
//        String submitter = (String) runtimeService.getVariable(processInstanceId, "initiator");
//        if (!currentUser.equals(submitter) && !currentUser.equals(currentTask.getAssignee())) {
//            throw new FlowableException("用户无权限驳回");
//        }

        // 3. 查询历史任务节点（按时间正序排列）
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();

        // 获取流程定义ID
        String processDefinitionId = getProcessDefinitionIdByTask(taskId);

        // 4. 过滤不可驳回的节点
        List<Map<String, String>> revokableNodes = new ArrayList<>();
        for (HistoricTaskInstance task : historicTasks) {
            if (taskId.equals(task.getId())) {
                continue;
            }
            String taskDefKey = task.getTaskDefinitionKey();
            String nodeType = getNodeType(processDefinitionId, taskDefKey);

            // 添加到可驳回列表
            Map<String, String> nodeInfo = new HashMap<>();
            nodeInfo.put("taskKey", task.getTaskDefinitionKey());
            nodeInfo.put("taskName", task.getName());
            nodeInfo.put("type", nodeType);
            revokableNodes.add(nodeInfo);
        }

        return revokableNodes;
    }

    /**
     * 获取节点类型
     */
    private String getNodeType(String processDefinitionId, String taskDefKey) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowElement element = bpmnModel.getFlowElement(taskDefKey);
        return element.getClass().getSimpleName(); // 返回 UserTask/ServiceTask 等类型
    }


    /**
     * 获取下一节点
     *
     * @param flowTaskVo 任务
     */
    @Override
    public FlowNextDto getNextFlowNode(FlowTaskVo flowTaskVo) {
        // Step 1. 获取当前节点并找到下一步节点
        Task task = taskService.createTaskQuery().taskId(flowTaskVo.getTaskId()).singleResult();
        FlowNextDto flowNextDto = new FlowNextDto();
        if (Objects.nonNull(task)) {
            // Step 2. 获取当前流程所有流程变量(网关节点时需要校验表达式)
            Map<String, Object> variables = taskService.getVariables(task.getId());
            List<UserTask> nextUserTask = FindNextNodeUtil.getNextUserTasks(repositoryService, task, variables);
            if (CollectionUtils.isNotEmpty(nextUserTask)) {
                for (UserTask userTask : nextUserTask) {
                    MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                    // 会签节点
                    if (Objects.nonNull(multiInstance)) {
                        List<SysUser> list = remoteuserservice.selectUserList(new SysUser(), SecurityConstants.INNER).getData();

                        flowNextDto.setVars(ProcessConstants.PROCESS_MULTI_INSTANCE_USER);
                        flowNextDto.setType(ProcessConstants.PROCESS_MULTI_INSTANCE);
                        flowNextDto.setUserList(list);
                    } else {

                        // 读取自定义节点属性 判断是否是否需要动态指定任务接收人员、组
                        String dataType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_DATA_TYPE);
                        String userType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_USER_TYPE);

                        // 处理加载动态指定下一节点接收人员信息
                        if (ProcessConstants.DATA_TYPE.equals(dataType)) {
                            // 指定单个人员
                            if (ProcessConstants.USER_TYPE_ASSIGNEE.equals(userType)) {
                                List<SysUser> list = remoteuserservice.selectUserList(new SysUser(), SecurityConstants.INNER).getData();
                                flowNextDto.setVars(ProcessConstants.PROCESS_APPROVAL);
                                flowNextDto.setType(ProcessConstants.USER_TYPE_ASSIGNEE);
                                flowNextDto.setUserList(list);
                            }
                            // 候选人员(多个)
                            if (ProcessConstants.USER_TYPE_USERS.equals(userType)) {
                                List<SysUser> list = remoteuserservice.selectUserList(new SysUser(), SecurityConstants.INNER).getData();

                                flowNextDto.setVars(ProcessConstants.PROCESS_APPROVAL);
                                flowNextDto.setType(ProcessConstants.USER_TYPE_USERS);
                                flowNextDto.setUserList(list);
                            }
                            // 候选组
                            if (ProcessConstants.USER_TYPE_ROUPS.equals(userType)) {
                                List<SysRole> sysRoles = remoteuserservice.selectRoleAll(new SysRole(), SecurityConstants.INNER).getData();

                                flowNextDto.setVars(ProcessConstants.PROCESS_APPROVAL);
                                flowNextDto.setType(ProcessConstants.USER_TYPE_ROUPS);
                                flowNextDto.setRoleList(sysRoles);
                            }
                        } else {
                            flowNextDto.setType(ProcessConstants.FIXED);
                        }
                    }
                }
            }
        }
        return flowNextDto;
    }

}
