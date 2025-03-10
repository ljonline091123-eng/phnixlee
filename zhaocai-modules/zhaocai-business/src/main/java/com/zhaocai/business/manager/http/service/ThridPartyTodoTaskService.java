package com.zhaocai.business.manager.http.service;

import com.alibaba.fastjson2.JSON;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.dto.res.PushThirdPartyTodoTaskResponseDTO;
import com.zhaocai.common.core.utils.ListUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ssy
 * @date 2024/8/5 10:43
 */
@Slf4j
@Service
public class ThridPartyTodoTaskService {

    @Async(value = "businessExecutor")
    public void pushTodoTask(PushThirdPartyTodoTaskRequestDTO requestDTO){
        List<PushThirdPartyTodoTaskSonRequestDTO> messageList = requestDTO.getMessageList();
        String authorization = requestDTO.getAuthorization();
        List<List<PushThirdPartyTodoTaskSonRequestDTO>> dataList = ListUtil.splitList(messageList, 100);

        for (List<PushThirdPartyTodoTaskSonRequestDTO> data : dataList) {
            PushThirdPartyTodoTaskRequestDTO taskRequestDTO = new PushThirdPartyTodoTaskRequestDTO();
            taskRequestDTO.setMessageList(data);
            taskRequestDTO.setAuthorization(authorization);
            pushTask(taskRequestDTO);
        }
    }

    public void pushTask(PushThirdPartyTodoTaskRequestDTO requestDTO){
//        PushThirdPartyTodoTaskResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.WAIT_HDL_HANDLE,
//                PushThirdPartyTodoTaskResponseDTO.class, requestDTO, requestDTO.getMessageList());
//        log.info("[推送第三方待办数据传输] - 请求结果为:{}", JSON.toJSONString(responseDTO));
    }

    /** 组装请求参数 */
    private void assembleRequestDTO(PushThirdPartyTodoTaskRequestDTO requestDTO){
    }

}
