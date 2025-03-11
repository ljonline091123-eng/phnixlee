package com.zhaocai.business.pub.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.PlatAreaDivision;
import com.zhaocai.business.manager.http.dto.req.UnderlyingPlatformBaseDTO;
import com.zhaocai.business.manager.http.dto.res.UnderlingResultData;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.pub.domain.ProjectWork;
import com.zhaocai.business.pub.mapper.ProjectWorkMapper;
import com.zhaocai.business.pub.service.IProjectWorkService;
import com.zhaocai.business.pub.vo.req.ProjectVO;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.Map2ObjUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import com.zhaocai.business.pub.mapper.ProjectMapper;
import com.zhaocai.business.pub.domain.Project;
import com.zhaocai.business.pub.service.IProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.crypto.Data;

/**
 * 项目Service业务层处理
 *
 * @author cff
 * @date 2024-09-26
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper,Project> implements IProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;


    @Autowired
    private IProjectWorkService iProjectWorkService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectWorkMapper projectWorkMapper;

    /**
     *接收项目
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean receiptProject() {
        boolean flat=true;
//        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + UnderlingPlatformUrlEnum.GET_PROJECT_ALL.getUrl())
//                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
//                .build();

        UriComponents uriComponents = null;
        try {
            UnderlyingPlatformBaseDTO requestDTO = new UnderlyingPlatformBaseDTO();
            HttpHeaders headers = new HttpHeaders();
            // 授权码
            headers.add("authorization", requestDTO.getAuthorization());
            UnderlingResultData<List<LinkedHashMap>> response = RestTemplateUtils.getForObject2Header(uriComponents.toString(), UnderlingResultData.class, headers);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                List<LinkedHashMap> mapList = response.getData();
                List<ProjectVO> list=new ArrayList<>();
                //数据封装
                projectEncapsulation(mapList, list);
                log.info("数据封装");
                //项目数据保存，以及详情
                flat=projectDataSave(list);
            }
        } catch (Exception ex) {
            flat=false;
            log.error("第三方项目列表接口获取失败:{}", ex.getMessage());
            ex.printStackTrace();
        }
        return flat;
    }

    /**
     * 项目数据保存，以及详情
     * @param list
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean projectDataSave(List<ProjectVO> list) {
        try {
            List<ProjectVO> projectList=projectMapper.selectProjectList(new Project());
            List<ProjectVO> oldProjectList = list.stream()
                    .filter(e1 -> projectList.stream().anyMatch(e2 -> e2.getThirdId().equals(e1.getThirdId())))
                    .collect(Collectors.toList());
            list.removeIf(item -> projectList.stream().anyMatch(otherItem -> otherItem.getThirdId().equals(item.getThirdId())));
            //新项目数据
            newProjectList(list);
            //老数据
            oldProjectList(oldProjectList);
        }catch (Exception e){
            e.printStackTrace();
            log.error("第三方项目列表接口获取失败:{}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 老数据
     * @param oldProjectList
     */
    private void oldProjectList(List<ProjectVO> oldProjectList) {
        if (!CollectionUtils.isEmpty(oldProjectList)) {
            //项目详情list
            List<ProjectWork> projectWorksList=new ArrayList<>();
            //项目数据保存
            for(ProjectVO tem:oldProjectList){
                Project project=new Project();
                BeanUtils.copyBeanProp(project,tem);
                int projectFlat=projectMapper.updateProject(project);

                Project selectById= projectMapper.selectProjectById(project.getThirdId());
                if(projectFlat>0){
                    //删除老数据(因为第三方没传明细id无法匹对只能删除更新最新数据)
                    projectWorkMapper.deleteProjectWorkByThirdId(project.getThirdId());
                    //封装详情数据
                    List<ProjectWork> list=tem.getProjectWorksList();
                    //保留新数据
                    if(!CollectionUtils.isEmpty(list)) {
                        for(ProjectWork newTem:list){
                            ProjectWork projectWork = new ProjectWork();
                            BeanUtils.copyBeanProp(projectWork, newTem);
                            projectWork.setParntId(String.valueOf(selectById.getId()));
                            projectWork.setThirdId(String.valueOf(project.getThirdId()));
                            projectWorksList.add(projectWork);
                        }
                    }
                }
            }
            //项目详情数据保存
            if (!CollectionUtils.isEmpty(projectWorksList)) {
                iProjectWorkService.saveBatch(projectWorksList);
            }
        }
    }

    /**
     * 新项目数据
     * @param list
     */
    private void newProjectList(List<ProjectVO> list) {
        if (!CollectionUtils.isEmpty(list)) {
             //项目详情list
             List<ProjectWork> projectWorksList=new ArrayList<>();
             //项目数据保存
             for(ProjectVO tem:list){
                 Project project=new Project();
                 BeanUtils.copyBeanProp(project,tem);
                 boolean projectFlat=this.save(project);
                 if(projectFlat){
                     //封装详情数据
                     if(!CollectionUtils.isEmpty(tem.getProjectWorksList())) {
                         for(ProjectWork tem1:tem.getProjectWorksList()){
                             ProjectWork projectWork = new ProjectWork();
                             BeanUtils.copyBeanProp(projectWork, tem1);
                             projectWork.setParntId(String.valueOf(project.getId()));
                             projectWork.setThirdId(String.valueOf(project.getThirdId()));
                             projectWorksList.add(projectWork);
                         }
                     }
                 }
             }
             //项目详情数据保存
             if (!CollectionUtils.isEmpty(projectWorksList)) {
                 iProjectWorkService.saveBatch(projectWorksList);
             }
         }
    }

    /**
     * 数据封装
     * @param mapList
     * @param list
     */
    private void projectEncapsulation(List<LinkedHashMap> mapList, List<ProjectVO> list) {
        if (!CollectionUtils.isEmpty(mapList)) {
            for (LinkedHashMap map : mapList) {
                Project projectMode = convertToObject(map, Project.class);
                ProjectVO project=new ProjectVO();
                BeanUtils.copyBeanProp(project,projectMode);
                project.setThirdId((String) map.get("id"));
                List<LinkedHashMap> worksList =(List<LinkedHashMap>)map.get("projectWorksList");
                if(!CollectionUtils.isEmpty(worksList)){
                    List<ProjectWork> projectWorksList=new ArrayList<>();
                    for(LinkedHashMap hashMap : worksList){
                        ProjectWork projectWork = convertToObject(hashMap, ProjectWork.class);
                        projectWork.setThirdId((String) map.get("id"));
                        projectWorksList.add(projectWork);
                    }
                    project.setProjectWorksList(projectWorksList);
                }
                list.add(project);
            }
        }
    }

    /**
     * 单独转换其他类型
     * @param map
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T convertToObject(LinkedHashMap<String, Object> map, Class<T> clazz) {
        T instance;
        try {
            instance = clazz.newInstance();
            for (String key : map.keySet()) {
                try {
                    Field field = clazz.getDeclaredField(key);
                    if(field.getGenericType().equals(BigDecimal.class)){
                        //Double
                        if(map.get(key) instanceof Double) {
                            field.setAccessible(true);
                            field.set(instance, map.get(key) != null && map.get(key) != "" ? BigDecimal.valueOf((Double) map.get(key)) : BigDecimal.valueOf(0));
                        //Integer
                        }else if(map.get(key) instanceof Integer){
                            field.setAccessible(true);
                            field.set(instance, map.get(key) != null && map.get(key) != "" ? BigDecimal.valueOf((Integer) map.get(key)) : BigDecimal.valueOf(0));
                        }
                    }else {
                        //时间类型
                        if (isValidTime((String) map.get(key))) {
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                            field.setAccessible(true);
                            field.set(instance, map.get(key) != null && map.get(key) != "" ? formatter.parse((String) map.get(key)) : null);
                        } else{
                            field.setAccessible(true);
                            field.set(instance, map.get(key));
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // 忽略不存在的属性
                    // 也可以选择记录日志或抛出异常
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instance;
    }

    /**
     * 判断是否是时间字符串
     * @param timeStr
     * @return
     */
    public static boolean isValidTime(String timeStr) {
        try {
            if(StringUtils.isNotNull(timeStr)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                formatter.parse(timeStr);
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
