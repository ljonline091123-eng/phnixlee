package com.zhaocai.flowable.mapper;

import com.zhaocai.flowable.domain.dto.FlowTaskDto;
import com.zhaocai.flowable.domain.dto.HistoricProcessInstanceDTO;
import com.zhaocai.flowable.domain.dto.HistoricTaskInstanceDTO;

import java.util.List;

/**
 * @Description 历史任务数据层
 * @Author Acechengui
 * @Date Created in 2023/6/23
 */
public interface FlowHistericTaskMapper {

    List<HistoricTaskInstanceDTO> selectFlowHistericTaskInstance(FlowTaskDto param);

    List<HistoricTaskInstanceDTO> selectFlowHistericTaskInstanceV2(FlowTaskDto param);

    Integer selectFlowHistericTaskInstanceCount(FlowTaskDto param);

    List<HistoricProcessInstanceDTO> selectFlowHistoricProcessInstance(FlowTaskDto param);

    Integer selectFlowHistoricProcessInstanceCount(FlowTaskDto param);

}
