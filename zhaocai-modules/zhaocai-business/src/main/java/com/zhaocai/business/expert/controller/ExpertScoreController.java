package com.zhaocai.business.expert.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.expert.service.IExpertScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 专家评分Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/score")
public class ExpertScoreController extends BladeController
{
    @Autowired
    private IExpertScoreService expertScoreService;

}
