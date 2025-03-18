package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.BusinessCodeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.mapper.MinProjectMapper;
import com.zhaocai.business.procurement.service.IContractPlanningService;
import com.zhaocai.business.procurement.service.IMinProjectDictProjectTypeService;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectDetailVO;
import com.zhaocai.business.procurement.vo.res.MinProjectListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.business.pub.service.IBusinessCodeService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最小核算项目信息Service业务层处理
 *
 * @author WH
 * @date 2024-07-16
 */
@Service
public class MinProjectServiceImpl extends ServiceImpl<MinProjectMapper, MinProject> implements IMinProjectService {

    @Autowired
    private ContractPlanService contractPlanService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Autowired
    private IMinProjectDictProjectTypeService minProjectDictProjectTypeService;

    @Autowired
    private IBusinessCodeService businessCodeService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Resource
    private MinProjectMapper minProjectMapper;


    /*
    * 新增、修改项目-保存项目信息
    * */
    @Override
    public MinProject saveProjectInfo(MinProjectDetailResponseDTO projectDetail) {
        //新增项目时时，生成最小核算项目编号
        if(StringUtils.isEmpty(projectDetail.getMinAccountCode())){
            projectDetail.setMinAccountCode(getMinAccountdCode());
        }
        if(projectDetail.getProjectDepartmentId() != null) {
            SysDept sysDept = remoteSystemService.getByThridDeptId(projectDetail.getProjectDepartmentId(), SecurityConstants.INNER);
            ValidateUtils.isNullException(sysDept, "该项目所归属项目部对应的组织机构不存在，请确认");
            projectDetail.setProjectDepartment(sysDept.getDeptName());
        }

        return saveMinProject(projectDetail);
    }


    @Override
    public MinProject saveMinProject(MinProjectDetailResponseDTO projectDetail) {
        synchronized (projectDetail.getMinAccountCode()) {
            MinProject saveMinProject = BeanCopierUtil.copyBean(projectDetail, MinProject.class);

            if (null != projectDetail.getId()) {
                saveMinProject.setMinProjectId(projectDetail.getId());
            }
            if (StringUtils.isNoneBlank(projectDetail.getPrjAddr())) {
                List<String> list = JSONArray.parseArray(projectDetail.getPrjAddr(), String.class);
                // TODO 省市区的需要再次确认
                if (CollectionUtil.isNotEmpty(list) && list.size() > 2) {
                    // 有 3 位，为省、市、区
                    saveMinProject.setPrjAddrProvince(list.get(0));
                    saveMinProject.setPrjAddrCity(list.get(1));
                    saveMinProject.setPrjAddrRegion(list.get(2));
                } else if (CollectionUtil.isNotEmpty(list) && list.size() > 1) {
                    // 有 2 位，为市、区
                    saveMinProject.setPrjAddrProvince(list.get(0));
                    saveMinProject.setPrjAddrCity(list.get(0));
                    saveMinProject.setPrjAddrRegion(list.get(1));
                }
            }

            MinProject minProject = super.getOne(new LambdaQueryWrapper<MinProject>()
                    .eq(MinProject::getMinAccountCode, projectDetail.getMinAccountCode()));
            if (minProject != null) {
                saveMinProject.setId(minProject.getId());
            } else {
                saveMinProject.setId(null);
            }

            super.saveOrUpdate(saveMinProject);
            return saveMinProject;
        }
    }

    /**
     * 获取采项目编码
     * @return
     */
    private String getMinAccountdCode() {
        return "SG" + LocalDate.now().getYear() + businessCodeService.getBusinessCode(BusinessCodeEnum.Project);
    }

    /**
     * 查询项目详情
     * @return
     */
    @Override
    public MinProjectDetailVO getMinProjectById(Long id) {
        if (NumberUtil.isNullOrZero(id)) {
            throw new ParamValidateException("项目id不能为空");
        }

        MinProject minProject = super.getOne(new LambdaQueryWrapper<MinProject>()
                .eq(MinProject::getId, id));

        MinProjectDetailVO minProjectDetailVO = BeanCopierUtil.copyBean(minProject, MinProjectDetailVO.class);
        if (StringUtils.isBlank(minProjectDetailVO.getManagementOrgId())) {
            throw new ParamValidateException("该项目所归属管理组织为空，请确认");
        }

//        // 获取我们系统里面对应的机构 id
//        SysDept sysDept = remoteSystemService.getByThridDeptId(minProjectDetailVO.getManagementOrgId(), SecurityConstants.INNER);
//        ValidateUtils.isNullException(sysDept, "该项目所归属管理组织对应的组织机构不存在，请确认");
//        minProjectDetailVO.setDeptId(sysDept.getDeptId());
        String prjStateText = sysDictDataService.getLabel(DictBizEnum.UNDERLING_PROJECT_FORMAT.getName(),minProjectDetailVO.getPrjState());
        String prjManageModelText = sysDictDataService.getLabel(DictBizEnum.PROJECT_MANAGE_MODEL.getName(),minProjectDetailVO.getPrjManageModel());
        String contractingModelText = sysDictDataService.getLabel(DictBizEnum.PROJECT_CONTRACTING_MODEL.getName(),minProjectDetailVO.getContractingModel());
        String stateText = sysDictDataService.getLabel(DictBizEnum.sys_project_status.getName(),minProjectDetailVO.getState());
        String moneySecText = sysDictDataService.getLabel(DictBizEnum.project_funds_source.getName(),minProjectDetailVO.getMoneySec());
        String zbTypeText = sysDictDataService.getLabel(DictBizEnum.sys_contracting_method.getName(),minProjectDetailVO.getZbType());
        minProjectDetailVO.setPrjStateText(prjStateText);
        minProjectDetailVO.setPrjManageModelText(prjManageModelText);
        minProjectDetailVO.setContractingModelText(contractingModelText);
        minProjectDetailVO.setStateText(stateText);
        minProjectDetailVO.setMoneySecText(moneySecText);
        minProjectDetailVO.setZbTypeText(zbTypeText);
        if (StringUtils.isNotBlank(minProjectDetailVO.getPrgType())) {
            minProjectDetailVO.setPrgType(formatString(minProjectDetailVO.getPrgType()));
        }

        return minProjectDetailVO;
    }

    //去除中括号和双引号
    public static String formatString(String inputStr) {
        // 去除字符串的开头和结尾的方括号
        String trimmedStr = inputStr.replaceAll("\\[|\\]", "");
        // 去除所有的双引号
        String noQuotesStr = trimmedStr.replaceAll("\"", "");
        // 去除所有的单引号
        String noApostrophesStr = noQuotesStr.replaceAll("'", "");
        // 返回格式化后的字符串
        return noApostrophesStr;
    }

    @Override
    public MinProjectVO getMinProjectByMinAccountCode(String projectCode) {
        if (StringUtils.isBlank(projectCode)) {
            throw new ParamValidateException("最小核算项目编码不能为空");
        }
        QueryWrapper<MinProject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("min_account_code",projectCode);
        queryWrapper.eq("del_flag","0");
        MinProject minProject = minProjectMapper.selectOne(queryWrapper);

        MinProjectVO minProjectVO = BeanCopierUtil.copyBean(minProject, MinProjectVO.class);
        if (StringUtils.isBlank(minProjectVO.getManagementOrgId())) {
            throw new ParamValidateException("该项目所归属管理组织为空，请确认");
        }

        // 获取我们系统里面对应的机构 id
        SysDept sysDept = remoteSystemService.getByThridDeptId(minProjectVO.getManagementOrgId(), SecurityConstants.INNER);
        ValidateUtils.isNullException(sysDept, "该项目所归属管理组织对应的组织机构不存在，请确认");
        minProjectVO.setDeptId(sysDept.getDeptId());
        return minProjectVO;
    }


    @Override
    public List<MinProjectVO> getMinProjectList(String managementOrgId) {
        QueryWrapper<MinProject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("management_org_id", managementOrgId);
        queryWrapper.orderByAsc("min_account_code");
        List<MinProject> minProjectList = super.list(queryWrapper);
        List<MinProjectVO> minProjectVOS = new ArrayList<>();
        for (MinProject minProject : minProjectList) {
            MinProjectVO minProjectVO = BeanCopierUtil.copyBean(minProject, MinProjectVO.class);
            minProjectVOS.add(minProjectVO);
        }
        return minProjectVOS;
    }

    @Override
    public PageResult<MinProjectListVO> getProjectListByQuery(MinProjectListRequestDTO requestDTO) {
//        SysDept sysDept = remoteSystemService.getInfo(requestDTO.getDeptId(),SecurityConstants.INNER);

        if(!StringUtils.isBlank(requestDTO.getDeptId())){
            requestDTO.setManagementOrgId(requestDTO.getDeptId());
        }

        // 执行分页查询
        IPage<MinProject> minProjectPage = baseMapper.selectListPage(requestDTO.toMybatisPage(), requestDTO);



        // 将查询结果转换为 VO 对象
        List<MinProjectListVO> minProjectVOS = minProjectPage.getRecords().stream()
                .map(minProject -> BeanCopierUtil.copyBean(minProject, MinProjectListVO.class))
                .collect(Collectors.toList());

        for (MinProjectListVO minProjectListVO : minProjectVOS){
            String prgTypeText = minProjectDictProjectTypeService.getDictProjectTypeName(minProjectListVO.getPrgType());
            minProjectListVO.setPrgTypeText(prgTypeText);
        }

        // 返回分页结果
        return new PageResult<>(minProjectVOS, (int) minProjectPage.getTotal());
    }

    @Override
    public int deleteProject(Long id) {
        MinProject minProject = super.getById(id);
        ValidateUtils.isNullException(minProject,"该项目不存在");
        return baseMapper.deleteMinProjectById(id);
    }



}
