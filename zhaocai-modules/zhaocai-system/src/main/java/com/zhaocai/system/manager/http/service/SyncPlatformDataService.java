package com.zhaocai.system.manager.http.service;

import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.enums.UserTypeEnum;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.utils.ListUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.manager.http.dto.PlatDept;
import com.zhaocai.system.manager.http.dto.PlatUser;
import com.zhaocai.system.service.ISysConfigService;
import com.zhaocai.system.service.ISysDeptService;
import com.zhaocai.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/7/9 17:39
 */
@Service
public class SyncPlatformDataService {
    private static final Logger log = LoggerFactory.getLogger(SyncPlatformDataService.class);

    @Autowired
    private PlatUserService platUserService;
    @Autowired
    private PlatOrgService platOrgService;

    @Autowired
    private ISysDeptService sysDeptService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysConfigService configService;

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncDept(){
        //获取全量部门信息
        List<PlatDept> depts = platOrgService.allDepts();

        if (CollectionUtils.isEmpty(depts)){
            throw new ServiceException("获取部门数据不成功");
        }
        //删除已经存在的部门数据
//        boolean delRes = sysDeptService.deleteSyncDept();

        List<List<PlatDept>> splitList = ListUtil.splitList(depts, 1000);
        for (List<PlatDept> platDepts : splitList){

            //新增获取到的全量部门数据
            List<SysDept> sysDeptsAdd = new ArrayList<>();
            List<SysDept> sysDeptsUpdate = new ArrayList<>();
            List<String> deptIdList = platDepts.stream().map(PlatDept::getDeptId).collect(Collectors.toList());
            /* 根据第三方部门id查询 我们自己数据库已经同步的数据 */
            List<SysDept> sysDepts = sysDeptService.getListByThridDeptId(deptIdList);
            /* 我们的部门数据，对第三方部门id去重。 */
            Map<String, SysDept> deptMap = sysDepts.stream().collect(Collectors.toMap(SysDept::getThridDeptId, Function.identity()));
            platDepts.forEach(item -> {
                SysDept sysDept;
                /* 对比的是第三方的部门id */
                if (deptMap.containsKey(item.getDeptId())){
                    /* 赋值我们原来的数据库的值。我们的主键是dept_id和这个item.getDeptId()不一样，item.getDeptId()是第三方部门id */
                    sysDept = deptMap.get(item.getDeptId());
                    //初始化父级关系和祖级
                    sysDept.setParentId(0L);
                    /** 这个祖级是根据部门往上查的getDeptId将他join.toString.{@link com.zhaocai.system.service.impl.SysDeptServiceImpl#insertDept} */
                    sysDept.setAncestors("");
                    sysDeptsUpdate.add(sysDept);
                } else {
                    sysDept = new SysDept();
                    sysDeptsAdd.add(sysDept);
                }
                //单位长度为10，且1开头就是一级单位，2开头就是二级单位
                if (item.getDeptId().length() == 10 && item.getDeptId().startsWith("1")){
                    sysDept.setThridOrgLevel(NumberConstant.ONE);
                } else if (item.getDeptId().length() == 10 && item.getDeptId().startsWith("2")){
                    sysDept.setThridOrgLevel(NumberConstant.TWO);

                  /* 三级单位先不加，因为有的业务是直接用 *.thrid_org_level IS NOT NULL 来获取二级单位。 */
//                }else{
//                    // 判断三级单位的编码规则
//                    if (item.getDeptId().length() == 10 &&
//                            !item.getDeptId().startsWith("000", 4) &&
//                            item.getDeptId().startsWith("000", 7)) {
//                        // 情况一：10位编码，第5-7位为非000，第8-10位为000
//                        sysDept.setThridOrgLevel(NumberConstant.THREE);
//                    } else if (item.getDeptId().length() == 13) {
//                        // 情况二：编码位数为13位
//                        sysDept.setThridOrgLevel(NumberConstant.THREE);
//                    }
                }

                sysDept.setDeptName(StringUtils.isEmpty(item.getDeptName()) ? item.getRemark() : item.getDeptName());
                sysDept.setOrderNum(Integer.valueOf(item.getOrderNum()));
                sysDept.setPhone(item.getPhone());
                sysDept.setEmail(item.getEmail());
                sysDept.setLeader(item.getLeader());
                sysDept.setOrigin(Constants.SYNC_THIRD);
                //部门状态:0正常,1停用
                sysDept.setStatus("0");
                //删除标志（0代表存在 2代表删除）
//            sysDept.setParentName(item.getSuperMgmtHirchyLpCorpOrgName());
                sysDept.setThridDeptId(item.getDeptId());
                sysDept.setThridParentId(item.getParentId());
                sysDept.setThridOrgType(item.getOrgTypeCd());
                sysDept.setDelFlag("0");
            });
            boolean updateRes = sysDeptService.insertDepts(sysDeptsUpdate, false);
            boolean addRes = sysDeptService.insertDepts(sysDeptsAdd, true);

        }

        /*//新增获取到的全量部门数据
        List<SysDept> sysDeptsAdd = new ArrayList<>();
        List<SysDept> sysDeptsUpdate = new ArrayList<>();
//        platDepts = platDepts.subList(0, 100);
//        ALTER TABLE sys_dept AUTO_INCREMENT = 10000;用于数据库插入测试
        platDepts.stream().forEach(item -> {
            SysDept sysDept;
            SysDept sysDeptOld = sysDeptService.getByThridDeptId(item.getDeptId());
            if (!ObjectUtils.isEmpty(sysDeptOld)){
                sysDept = sysDeptOld;
                //初始化父级关系和祖级
                sysDept.setParentId(0L);
                sysDept.setAncestors("");
                sysDeptsUpdate.add(sysDept);
            } else {
                sysDept = new SysDept();
                sysDeptsAdd.add(sysDept);
            }
//            sysDept.setDeptId(Long.valueOf(item.getDeptId()));
//            sysDept.setParentId(Long.valueOf(item.getParentId()));
//            sysDept.setDeptName(item.getDeptName());
            sysDept.setDeptName(StringUtils.isEmpty(item.getRemark()) ? item.getDeptName() : item.getRemark());
            sysDept.setOrderNum(Integer.valueOf(item.getOrderNum()));
            sysDept.setPhone(item.getPhone());
            sysDept.setEmail(item.getEmail());
            sysDept.setLeader(item.getLeader());
            sysDept.setOrigin(Constants.SYNC_THIRD);
            //部门状态:0正常,1停用
            sysDept.setStatus("0");
            //删除标志（0代表存在 2代表删除）
//            sysDept.setParentName(item.getSuperMgmtHirchyLpCorpOrgName());
            sysDept.setThridDeptId(item.getDeptId());
            sysDept.setThridParentId(item.getParentId());
            sysDept.setThridOrgType(item.getOrgTypeCd());
        });
        boolean updateRes = sysDeptService.insertDepts(sysDeptsUpdate, false);
        boolean addRes = sysDeptService.insertDepts(sysDeptsAdd, true);*/

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncUser(){
        //获取全量用户信息
        List<PlatUser> users = platUserService.getPlatUser();

        if (CollectionUtils.isEmpty(users)){
            throw new ServiceException("获取用户数据不成功");
        }
        //删除已经存在的用户数据
//        Boolean delRes = sysUserService.deleteSyncUser();
        String password = configService.selectConfigByKey("sys.user.initPassword");

        List<List<PlatUser>> splitList = ListUtil.splitList(users, 1000);
        for (List<PlatUser> platUsers : splitList){

            //新增获取到的全量用户数据
            List<SysUser> sysUsersAdd = new ArrayList<>();
            List<SysUser> sysUsersUpdate = new ArrayList<>();
            List<String> deptIdList = platUsers.stream().map(PlatUser::getDeptId).collect(Collectors.toList());
            //批量获取到部门信息
            List<SysDept> sysDepts = sysDeptService.getListByThridDeptId(deptIdList);
            Map<String, SysDept> deptMap = sysDepts.stream().collect(Collectors.toMap(SysDept::getThridDeptId, Function.identity()));

            List<String> userIdList = platUsers.stream().map(PlatUser::getUserId).collect(Collectors.toList());
            //批量获取到用户信息
            List<SysUser> sysUsers = sysUserService.getListByThridUserId(userIdList);
            Map<String, SysUser> userMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getThridUserId, Function.identity()));

            platUsers.stream().forEach(item -> {
                SysUser sysUser;

                if (userMap.containsKey(item.getUserId())){
                    sysUser = userMap.get(item.getUserId());
                    sysUsersUpdate.add(sysUser);
                } else {
                    sysUser = new SysUser();
                    sysUsersAdd.add(sysUser);
                }

                if (deptMap.containsKey(item.getDeptId())){
                    sysUser.setDeptId(deptMap.get(item.getDeptId()).getDeptId());
                }

                sysUser.setThridUserId(item.getUserId());
                sysUser.setThridDeptId(item.getDeptId());
                sysUser.setThridOrgId(item.getBelongCurrLvlOrgCode());
                sysUser.setUserName(item.getUserName());
                sysUser.setNickName(item.getNickName());
                sysUser.setEmail(item.getEmail());
                sysUser.setPhonenumber(item.getPhonenumber());
                sysUser.setSex(item.getSex());
                sysUser.setPassword(SecurityUtils.encryptPassword(password));
                sysUser.setUserType(UserTypeEnum.PURCHASE.getName());
//            sysUser.setUserType(Constants.SYNC_THIRD);
                sysUser.setThridOrgName(item.getBelongCurrLvlOrg());
                //帐号状态（0正常 1停用）
                sysUser.setStatus("0");
                sysUser.setDelFlag("0");
            });
            Boolean updateRes = sysUserService.insertUsers(sysUsersUpdate, false);
            Boolean addRes = sysUserService.insertUsers(sysUsersAdd, true);

        }

        /*//新增获取到的全量用户数据
        String password = configService.selectConfigByKey("sys.user.initPassword");
        List<SysUser> sysUsersAdd = new ArrayList<>();
        List<SysUser> sysUsersUpdate = new ArrayList<>();
        platUsers.stream().forEach(item -> {
            SysUser sysUser;
            SysUser sysUserOld = sysUserService.getByThridUserId(item.getUserId());
            if (!ObjectUtils.isEmpty(sysUserOld)){
                sysUser = sysUserOld;
                sysUsersUpdate.add(sysUser);
            } else {
                sysUser = new SysUser();
                sysUsersAdd.add(sysUser);
            }

            SysDept sysDept = sysDeptService.getByThridDeptId(item.getDeptId());
            if (!ObjectUtils.isEmpty(sysDept)){
                sysUser.setDeptId(sysDept.getDeptId());
            }
            sysUser.setThridUserId(item.getUserId());
            sysUser.setThridDeptId(item.getDeptId());
            sysUser.setThridOrgId(item.getBelongCurrLvlOrgCode());
            sysUser.setUserName(item.getUserName());
            sysUser.setNickName(item.getNickName());
            sysUser.setEmail(item.getEmail());
            sysUser.setPhonenumber(item.getPhonenumber());
            sysUser.setSex(item.getSex());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            sysUser.setUserType(UserTypeEnum.PURCHASE.getName());
//            sysUser.setUserType(Constants.SYNC_THIRD);
            sysUser.setThridOrgName(item.getBelongCurrLvlOrg());
            //帐号状态（0正常 1停用）
            sysUser.setStatus("0");
        });
        Boolean updateRes = sysUserService.insertUsers(sysUsersUpdate, false);
        Boolean addRes = sysUserService.insertUsers(sysUsersAdd, true);*/

        return true;
    }

}
