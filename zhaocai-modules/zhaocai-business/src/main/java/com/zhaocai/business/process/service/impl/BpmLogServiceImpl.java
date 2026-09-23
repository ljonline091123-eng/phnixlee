package com.zhaocai.business.process.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.process.domain.BpmLog;
import com.zhaocai.business.process.service.IBpmLogService;
import com.zhaocai.business.process.mapper.BpmLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class BpmLogServiceImpl extends ServiceImpl<BpmLogMapper, BpmLog> implements IBpmLogService {

}
