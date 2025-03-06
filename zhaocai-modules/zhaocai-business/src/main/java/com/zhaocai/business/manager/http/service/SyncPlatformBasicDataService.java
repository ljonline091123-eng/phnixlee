package com.zhaocai.business.manager.http.service;

import com.zhaocai.business.manager.http.dto.PlatAreaDivision;
import com.zhaocai.business.manager.http.dto.PlatCountry;
import com.zhaocai.business.manager.http.dto.PlatDept;
import com.zhaocai.business.manager.http.dto.PlatUser;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.domain.Country;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.service.IAreaDivisionService;
import com.zhaocai.business.pub.service.IBankService;
import com.zhaocai.business.pub.service.ICountryService;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.utils.ListUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import com.zhaocai.system.api.system.RemoteUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/9 17:39
 */
@Service
public class SyncPlatformBasicDataService {
    private static final Logger log = LoggerFactory.getLogger(SyncPlatformBasicDataService.class);

    @Autowired
    private PlatUserService platUserService;
    @Autowired
    private PlatOrgService platOrgService;
    @Autowired
    private IAreaDivisionService areaDivisionService;
    @Autowired
    private PlatAreaDivisionService platAreaDivisionService;
    @Autowired
    private RemoteSystemService remoteSystemService;
    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private PlatCountryService platCountryService;
    @Autowired
    private ICountryService countryService;

    @Autowired
    private IBankService bankService;



    @Transactional(rollbackFor = Exception.class)
    public Boolean syncDept(){
        //获取全量部门信息
        List<PlatDept> platDepts = platOrgService.allDepts();
        /*List<PlatDept> platDepts = new ArrayList<>();
        PlatDept platDept1 = new PlatDept();
        platDept1.setDeptName("湖南建设投资集团有限责任公司");
        platDept1.setLeader(null);
        platDept1.setDeptId("1000000000");
        platDept1.setOrderNum("1");
        platDept1.setUnifiedSociCrdtCd(null);
        platDept1.setSuperMgmtHirchyLpCorpOrgName(null);
        platDept1.setParentId("0");
        platDept1.setSuperMgmtHirchyLpCorpOrgCode(null);
        platDept1.setInterialId("15362351412098327060614055620953");
        platDept1.setPayTaxMainIndCd(null);
        platDept1.setPhone(null);
        platDept1.setSimpleName(null);
        platDept1.setOrgCode(null);
        platDept1.setOrgTypeCd(null);
        platDept1.setBelgMgmtScdLvlLpCorpOrgName(null);
        platDept1.setAddr(null);
        platDept1.setEmail(null);
        platDept1.setBelgMgmtScdLvlLpCorpOrgCode(null);
        PlatDept platDept2 = new PlatDept();
        platDept2.setDeptName("集团领导");
        platDept2.setLeader("蔡典维");
        platDept2.setDeptId("10000000000001");
        platDept2.setOrderNum("0");
        platDept2.setUnifiedSociCrdtCd(null);
        platDept2.setSuperMgmtHirchyLpCorpOrgName(null);
        platDept2.setParentId("1000000000");
        platDept2.setSuperMgmtHirchyLpCorpOrgCode(null);
        platDept2.setInterialId("11090350608136557484832239161486");
        platDept2.setPayTaxMainIndCd(null);
        platDept2.setPhone(null);
        platDept2.setSimpleName(null);
        platDept2.setOrgCode(null);
        platDept2.setOrgTypeCd("BM");
        platDept2.setBelgMgmtScdLvlLpCorpOrgName("湖南建设投资集团有限责任公司");
        platDept2.setAddr(null);
        platDept2.setEmail(null);
        platDept2.setBelgMgmtScdLvlLpCorpOrgCode("1000000000");
        platDepts.add(platDept1);
        platDepts.add(platDept2);*/

        if (CollectionUtils.isEmpty(platDepts)){
            throw new ServiceException("获取部门数据不成功");
        }
        //删除已经存在的部门数据
        boolean delRes = remoteSystemService.deleteSyncDept(SecurityConstants.INNER);

        //新增获取到的全量部门数据
        List<SysDept> sysDepts = new ArrayList<>(platDepts.size());
//        platDepts = platDepts.subList(0, 100);
//        ALTER TABLE sys_dept AUTO_INCREMENT = 10000;用于数据库插入测试
        platDepts.stream().forEach(item -> {
            SysDept sysDept = new SysDept();
//            sysDept.setDeptId(Long.valueOf(item.getDeptId()));
//            sysDept.setParentId(Long.valueOf(item.getParentId()));
            sysDept.setDeptName(item.getDeptName());
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
            sysDepts.add(sysDept);
        });
        boolean addRes = remoteSystemService.addList(sysDepts, SecurityConstants.INNER);

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncUser(){
        //获取全量用户信息
        List<PlatUser> platUsers = platUserService.getPlatUser();
        /*List<PlatUser> platUsers = new ArrayList<>();
        PlatUser platUser1 = new PlatUser();
        platUser1.setUserId("190500001");
        platUser1.setYglx("2");
        platUser1.setBelgDeptName("松雅湖项目领导班子");
        platUser1.setNickName("王灿");
        platUser1.setSex("1");
        platUser1.setDeptId("200100000000020190004001");
        platUser1.setPhonenumber("18975123777");
        platUser1.setUserName("18975123777");
        platUser1.setBelongCurrLvlOrgCode("200100000000420230013");
        platUser1.setJz("Y");
        platUser1.setPostName("项目经理");
        platUser1.setBelgDeptCode("2001000000004202300130001");
        platUser1.setBelongCurrLvlOrg("湖南建工集团有限公司长沙县松雅湖生态新城人才公寓项目工程总承包(EPC)项目经理部");
        platUser1.setEmail("");
        PlatUser platUser2 = new PlatUser();
        platUser2.setUserId("190500002");
        platUser2.setYglx("3");
        platUser2.setBelgDeptName("国网湖南长沙供电公司生产综合用房项目工程部");
        platUser2.setNickName("康众");
        platUser2.setSex("1");
        platUser2.setDeptId("200100000000020190004002");
        platUser2.setPhonenumber("15717491689");
        platUser2.setUserName("15717491689");
        platUser2.setBelongCurrLvlOrgCode("200100000000420230005");
        platUser2.setJz("N");
        platUser2.setPostName("施工员");
        platUser2.setBelgDeptCode("2001000000004202300050002");
        platUser2.setBelongCurrLvlOrg("湖南建工集团有限公司国网湖南长沙供电公司生产综合用房项目经理部");
        platUser2.setEmail("");
        platUsers.add(platUser1);
        platUsers.add(platUser2);*/

        if (CollectionUtils.isEmpty(platUsers)){
            throw new ServiceException("获取用户数据不成功");
        }
        //删除已经存在的用户数据
        Boolean delRes = remoteUserService.deleteSyncUser(SecurityConstants.INNER);

        //新增获取到的全量用户数据
        String password = remoteSystemService.configKeyStr("sys.user.initPassword");
        List<SysUser> sysUsers = new ArrayList<>(platUsers.size());
        platUsers.stream().forEach(item -> {
            SysUser sysUser = new SysUser();
            sysUser.setThridUserId(item.getUserId());
            sysUser.setThridDeptId(item.getDeptId());
            sysUser.setUserName(item.getUserName());
            sysUser.setNickName(item.getNickName());
            sysUser.setEmail(item.getEmail());
            sysUser.setPhonenumber(item.getPhonenumber());
            sysUser.setSex(item.getSex());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
//            sysUser.setUserType(UserTypeEnum.PURCHASE.getName());
            sysUser.setThridOrgName(item.getBelongCurrLvlOrg());
            sysUser.setUserType(Constants.SYNC_THIRD);
            //帐号状态（0正常 1停用）
            sysUser.setStatus("0");
            sysUsers.add(sysUser);
        });
        Boolean addRes = remoteUserService.addList(sysUsers, SecurityConstants.INNER);

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncAreaDivision(){
        //获取全量区域划分信息
        List<PlatAreaDivision> platAreaDivisions = platAreaDivisionService.getAreaDivisionList();
        if (CollectionUtils.isEmpty(platAreaDivisions)){
            throw new ServiceException("获取区域划分数据不成功");
        }

        //删除已经存在的区域划分数据
        Boolean delRes = areaDivisionService.deleteSyncAreaDivision();
        List<AreaDivision> areaDivisions = new ArrayList<>(platAreaDivisions.size());
        platAreaDivisions.stream().forEach(item -> {
            AreaDivision areaDivision = new AreaDivision();
            areaDivision.setId(Long.valueOf(item.getId()));
            areaDivision.setAreaCode(item.getCode());
            areaDivision.setAreaName(item.getName());
            areaDivision.setParentCode(item.getParentCode());
            areaDivision.setParentName(item.getParentName());
            areaDivision.setOrderNum(item.getOrderNum());
            areaDivision.setRemark(item.getRemark());
            areaDivision.setOrigin(Constants.SYNC_THIRD);
            areaDivisions.add(areaDivision);
        });
        Boolean addRes = areaDivisionService.saveBatch(areaDivisions, areaDivisions.size());

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncCountry(){
        //获取全量区域划分信息
        List<PlatCountry> countryList = platCountryService.getCountryList();
        if (CollectionUtils.isEmpty(countryList)){
            throw new ServiceException("获取国家和地区档案数据不成功");
        }

        //删除已经存在的国家和地区档案数据
        Boolean delRes = countryService.deleteSyncCountry();
        List<Country> countrys = new ArrayList<>(countryList.size());
        countryList.stream().forEach(item -> {
            Country country = new Country();
            country.setId(Long.valueOf(item.getId()));
            country.setThridId(item.getId());
            country.setArabicNumeralCode(item.getArabicNumeralCode());
            country.setChineseAsName(item.getChineseAsName());
            country.setChineseFullName(item.getChineseFullName());
            country.setEnglishAsName(item.getEnglishAsName());
            country.setEnglishFullName(item.getEnglishFullName());
            country.setThreeCharCode(item.getThreeCharCode());
            country.setTwoCharCode(item.getTwoCharCode());
            countrys.add(country);
        });
        Boolean addRes = countryService.saveBatch(countrys, countrys.size());

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean receiptAccount() {
        List<DwCdBank> bankLIst = bankService.getBankList();
        if (CollectionUtils.isEmpty(bankLIst)){
            throw new ServiceException("获取支行信息数据不成功");
        }
        bankService.deleteSyncBank();
        List<List<DwCdBank>> splitList = ListUtil.splitList(bankLIst, 1000);
        for (List<DwCdBank> platBanks : splitList) {
            log.info("同步用户数据中。。。。");
             bankService.saveBatch(platBanks, platBanks.size());
        }
        return true;
    }
}
