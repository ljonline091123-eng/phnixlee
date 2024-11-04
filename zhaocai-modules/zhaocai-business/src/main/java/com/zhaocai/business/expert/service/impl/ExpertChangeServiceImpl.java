package com.zhaocai.business.expert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.expert.domain.ExpertChange;
import com.zhaocai.business.expert.mapper.ExpertChangeMapper;
import com.zhaocai.business.expert.service.IExpertChangeService;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.common.core.web.bean.ResultData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 专家修改Service业务层处理
 */
@Service
public class ExpertChangeServiceImpl extends ServiceImpl<ExpertChangeMapper, ExpertChange> implements IExpertChangeService {


    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return null;
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return null;
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        return "";
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return null;
    }

    @Override
    public void processStart(Map<String, Object> variables) {

    }

    @Override
    public void processAuditPass(Map<String, Object> variables) {

    }
}
