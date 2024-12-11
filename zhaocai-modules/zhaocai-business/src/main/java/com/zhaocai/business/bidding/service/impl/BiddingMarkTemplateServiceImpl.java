package com.zhaocai.business.bidding.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingMarkCategory;
import com.zhaocai.business.bidding.domain.BiddingMarkItem;
import com.zhaocai.business.bidding.domain.BiddingMarkTemplate;
import com.zhaocai.business.bidding.mapper.BiddingMarkTemplateMapper;
import com.zhaocai.business.bidding.service.IBiddingMarkCategoryService;
import com.zhaocai.business.bidding.service.IBiddingMarkItemService;
import com.zhaocai.business.bidding.service.IBiddingMarkTemplateService;
import com.zhaocai.business.bidding.vo.req.BiddingMarkCategoryVO;
import com.zhaocai.business.bidding.vo.req.BiddingMarkItemVO;
import com.zhaocai.business.bidding.vo.req.BiddingMarkTemplateVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingMarkTemplateQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkCategoryDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkItemDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateListVO;
import com.zhaocai.business.common.enums.DeptEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.text.Convert;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评分模板Service业务层处理
 *
 * @author WH
 * @date 2024-06-18
 */
@Service
public class BiddingMarkTemplateServiceImpl extends ServiceImpl<BiddingMarkTemplateMapper,BiddingMarkTemplate> implements IBiddingMarkTemplateService {

    @Autowired
    private IBiddingMarkCategoryService biddingMarkCategoryService;
    @Autowired
    private IBiddingMarkItemService biddingMarkItemService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Override
    public BiddingMarkTemplateDetailVO detail(Long id) {
        BiddingMarkTemplate markTemplate = this.getById(id);
        ValidateUtils.isNullException(markTemplate, "评分模板数据为空");

        BiddingMarkTemplateDetailVO vo = BeanCopierUtil.copyBean(markTemplate, BiddingMarkTemplateDetailVO.class);
        vo.setStateText(vo.getState() == NumberConstant.ONE ? "启用" : "未启用");

        List<BiddingMarkCategory> markCategorys = biddingMarkCategoryService.list(new LambdaQueryWrapper<BiddingMarkCategory>()
                .eq(BiddingMarkCategory::getTemplateId, markTemplate.getId()));
        List<BiddingMarkCategoryDetailVO> markCategoryDatailVOList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(markCategorys)){
            for (BiddingMarkCategory markCategory : markCategorys){
                BiddingMarkCategoryDetailVO markCategoryVO = BeanCopierUtil.copyBean(markCategory, BiddingMarkCategoryDetailVO.class);
                markCategoryDatailVOList.add(markCategoryVO);

                List<BiddingMarkItem> markItems = biddingMarkItemService.list(new LambdaQueryWrapper<BiddingMarkItem>()
                        .eq(BiddingMarkItem::getCategoryId, markCategoryVO.getId()));
                List<BiddingMarkItemDetailVO> markItemDetailVOList = BeanCopierUtil.copyList(markItems, BiddingMarkItemDetailVO.class);
                List<BiddingMarkItemDetailVO> parentMarkItems = markItemDetailVOList.stream().filter(item -> item.getParentId() == null).collect(Collectors.toList());
                List<BiddingMarkItemDetailVO> subMarkItems = markItemDetailVOList.stream().filter(item -> item.getParentId() != null).collect(Collectors.toList());
                parentMarkItems.stream().forEach(item -> {
                    List<BiddingMarkItemDetailVO> resItems = subMarkItems.stream().filter(
                            subItem -> subItem.getParentId().equals(item.getId())).collect(Collectors.toList());
                    item.setSubBiddingMarkItemDetailVOList(resItems);
                });

                markCategoryVO.setMarkItemDetailVOList(parentMarkItems);
            }
        }
        vo.setMarkCategoryDatailVOList(markCategoryDatailVOList);
        return vo;
    }
    @Override
    public PageResult<BiddingMarkTemplateListVO> page(BiddingMarkTemplateQueryVO queryVO) {
        IPage<BiddingMarkTemplateListVO> iPage = baseMapper.page(queryVO.toMybatisPage(), queryVO);
        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<BiddingMarkTemplateListVO> fanListPage(BiddingMarkTemplateQueryVO queryVO) {
        IPage<BiddingMarkTemplateListVO> pages = fanList(queryVO,"1");
        return new PageResult<>(pages);
    }

    @Override
    public PageResult<BiddingMarkTemplateListVO> switchListPage(BiddingMarkTemplateQueryVO queryVO) {
        IPage<BiddingMarkTemplateListVO> pages =null;
        //类型判断
        if(StringUtils.isBlank(queryVO.getSwitchTemplateType())){
            throw new BusinessException("切换类型为空");
        }
        //类型为:1、通用模板 2、复用模板
        if(queryVO.getSwitchTemplateType().equals("1")){
            //deptId==1000000000作为 集团 判断依据 查他本身和二级单位
            pages = fanList(queryVO,"2");
        }else {
            pages = baseMapper.multiplexList(queryVO.toMybatisPage(),queryVO);
        }
        return new PageResult<>(pages);
    }

    @Nullable
    private IPage<BiddingMarkTemplateListVO> fanList(BiddingMarkTemplateQueryVO queryVO,String group) {
        String currUserTowLevelThridDeptId =  remoteSystemService.getTwoLevelDeptByDeptId
                (SecurityUtils.getSysUser().getDeptId(),SecurityConstants.INNER).getThridDeptId();
        // 获取所有二级组织及集团
        List<SysDept> sysDeptList = remoteSystemService.getTwoLevelDepts(SecurityConstants.INNER);
//        if (!currUserTowLevelThridDeptId.equals(UserConstants.GROUP_DEPT_ID)) {
//            //通用模板
//            if (group.equals("2")) {
//                sysDeptList = sysDeptList.stream().filter(item -> item.getThridOrgLevel() == NumberConstant.ONE ||
//                        item.getThridDeptId().equals(currUserTowLevelThridDeptId)).collect(Collectors.toList());
//            }else {
//                sysDeptList = sysDeptList.stream().filter(item ->
//                        item.getThridDeptId().equals(currUserTowLevelThridDeptId)).collect(Collectors.toList());
//                /* 权限控制到项目部 */
////                sysDeptList = remoteSystemService.getDeptByThridDeptId(currUserTowLevelThridDeptId,SecurityConstants.INNER);
//            }
//        }
        if (CollectionUtil.isNotEmpty(sysDeptList)) {
            List<String> deptIdList = sysDeptList.stream().map(dept -> dept.getDeptId()+"").collect(Collectors.toList());
            String id=String.join(",",deptIdList);
            String[] deptIds = Convert.toStrArray(id);
            queryVO.setUseUnit(deptIds);
            IPage<BiddingMarkTemplateListVO> pages = baseMapper.selectList(queryVO.toMybatisPage(),queryVO);
            return pages;
        }
        return null;
    }

    /**
     * 判断依据 查他本身和二级单位
     * @param pages
     * @param queryVO
     * @return
     */
    private IPage<BiddingMarkTemplateListVO> getTemplateListVOIPage(IPage<BiddingMarkTemplateListVO> pages,BiddingMarkTemplateQueryVO queryVO) {
        //查他本身和二级单位list
        List<SysDept> sysDeptList=remoteSystemService.getBySwitchListThridDeptId("1000000000", SecurityConstants.INNER);
        // 只保留公司
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x -> "syncthird".equals(x.getOrigin()) && !"X".equals(x.getThridOrgType()) && !"BM".equals(x.getThridOrgType()))
                .collect(Collectors.toList());
        List<String> deptIdList=new ArrayList<>();
        filterList.stream().forEach(item -> {
            deptIdList.add(item.getDeptId()+"");
        });
        String[] deptId = Convert.toStrArray(String.join(",", deptIdList));
        pages = baseMapper.switchList(queryVO.toMybatisPage(),deptId,queryVO);
        System.out.println(pages);
        return pages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrUpdate(BiddingMarkTemplateVO biddingMarkTemplateVO) {
        BiddingMarkTemplate markTemplate = BeanCopierUtil.copyBean(biddingMarkTemplateVO, BiddingMarkTemplate.class);
        boolean res;

        if (markTemplate.getId() == null){
            //1.新增评分模板信息
            res = this.save(markTemplate);
        } else {
            //修改评分模板信息
            res = this.updateById(markTemplate);
            this.removeMarkCategoryAndItem(markTemplate.getId());
        }

        //新增评分模板项信息
        List<BiddingMarkCategoryVO> markCategoryVOList = biddingMarkTemplateVO.getBiddingMarkCategoryVOList();
        List<BiddingMarkItem> subMarkItems = new ArrayList<>();
        for (BiddingMarkCategoryVO mcVO : markCategoryVOList){
            BiddingMarkCategory markCategory = BeanCopierUtil.copyBean(mcVO, BiddingMarkCategory.class);
            markCategory.setTemplateId(markTemplate.getId());
            biddingMarkCategoryService.save(markCategory);

            //3.新增模板评分项信息
            List<BiddingMarkItemVO> markItemVOList = mcVO.getBiddingMarkItemVOList();
            for (BiddingMarkItemVO markItemVO : markItemVOList){
                BiddingMarkItem markItem = BeanCopierUtil.copyBean(markItemVO, BiddingMarkItem.class);
                markItem.setCategoryId(markCategory.getId());
                biddingMarkItemService.save(markItem);

                List<BiddingMarkItemVO> subBiddingMarkItemVOList = markItemVO.getSubBiddingMarkItemVOList();
                if (CollectionUtil.isNotEmpty(subBiddingMarkItemVOList)){
                    for (BiddingMarkItemVO subMarkItemVO : subBiddingMarkItemVOList){
                        BiddingMarkItem subMarkItem = BeanCopierUtil.copyBean(subMarkItemVO, BiddingMarkItem.class);
                        subMarkItem.setCategoryId(markCategory.getId());
                        subMarkItem.setParentId(markItem.getId());
                        subMarkItems.add(subMarkItem);
                    }
                }
            }
        }
        biddingMarkItemService.saveBatch(subMarkItems);
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        this.removeMarkCategoryAndItem(id);
        return this.removeById(id);
    }

    @Override
    public boolean updateStatus(Long id, Integer state) {
        LambdaUpdateWrapper<BiddingMarkTemplate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(BiddingMarkTemplate::getState, state);
        updateWrapper.eq(BiddingMarkTemplate::getId, id);
        return update(updateWrapper);
    }


    private void removeMarkCategoryAndItem(Long templateId){
        List<BiddingMarkCategory> markCategorys = biddingMarkCategoryService.list(new LambdaQueryWrapper<BiddingMarkCategory>()
                .eq(BiddingMarkCategory::getTemplateId, templateId));
        List<Long> markCategoryIds = markCategorys.stream().map(BiddingMarkCategory::getId).collect(Collectors.toList());

        //删除评分模板项信息
        biddingMarkCategoryService.remove(new LambdaQueryWrapper<BiddingMarkCategory>()
                .eq(BiddingMarkCategory::getTemplateId, templateId));

        //删除模板评分项信息
        if (CollectionUtil.isNotEmpty(markCategoryIds)){
            biddingMarkItemService.remove(new LambdaQueryWrapper<BiddingMarkItem>()
                    .in(BiddingMarkItem::getCategoryId, markCategoryIds));
        }
    }


}
