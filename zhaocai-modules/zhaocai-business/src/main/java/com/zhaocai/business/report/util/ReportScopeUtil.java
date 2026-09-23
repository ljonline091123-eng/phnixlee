package com.zhaocai.business.report.util;

import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;

/**
 * 报表数据权限工具
 *
 * 招标率统计、供应商报表、问题报表三个报表不受顶部“单位-项目”选择框的限制，
 * 默认查询范围为【登录用户所属组织及其下级组织】：
 * - 集团账号（thrid_dept_id = 1000000000）：不限制，可查全部
 * - 其他账号：限制为登录用户所属组织
 *
 * 说明：登录用户信息里 sys_user.thrid_org_id 即该用户所属组织的 sys_dept.thrid_dept_id，
 *       sys_user.dept_id 即 sys_dept.dept_id（报表行级过滤用 dept_id / ancestors）。
 *
 * @author claude
 */
public class ReportScopeUtil {

    private ReportScopeUtil() {
    }

    private static SysUser getUser() {
        return SecurityUtils.getSysUser();
    }

    /**
     * 当前登录用户所属组织（token 里只带 dept_id / ancestors / dept_name 等基础字段）
     */
    public static SysDept getUserDept() {
        SysUser user = getUser();
        return user == null ? null : user.getDept();
    }

    /**
     * 报表默认的根组织（登录用户所属组织的 thrid_dept_id），取不到返回 null
     */
    public static String getDefaultOrgId() {
        SysUser user = getUser();
        return user == null ? null : user.getThridOrgId();
    }

    /**
     * 是否集团账号（集团账号查全部数据，不加组织限制）
     */
    public static boolean isGroupUser() {
        return UserConstants.GROUP_DEPT_ID.equals(getDefaultOrgId());
    }

    /**
     * 是否需要按组织限制数据范围（集团账号不需要）
     */
    public static boolean needScope() {
        return !isGroupUser();
    }

    /**
     * 目标组织是否在登录用户的数据权限范围内（本人所属组织及其下级）
     * 用于校验前端传入的组织 id，越权时由调用方回落到本人所属组织
     *
     * @param targetDept 待校验的组织
     */
    public static boolean inScope(SysDept targetDept) {
        if (isGroupUser()) {
            return true;
        }
        SysDept userDept = getUserDept();
        if (targetDept == null || userDept == null || targetDept.getDeptId() == null) {
            return false;
        }
        if (userDept.getDeptId().equals(targetDept.getDeptId())) {
            return true;
        }
        // sys_dept.ancestors 存的是祖先 dept_id 路径，本身的下级组织其 ancestors 以 用户组织路径 开头
        String userPath = userDept.getAncestors() + "," + userDept.getDeptId();
        String targetAncestors = targetDept.getAncestors();
        return StringUtils.isNotEmpty(targetAncestors)
                && (targetAncestors.equals(userPath) || targetAncestors.startsWith(userPath + ","));
    }

    /**
     * 行级过滤用的组织上限（sys_dept.dept_id）
     * 集团账号返回 null，表示不加组织限制
     */
    public static String getScopeDeptId() {
        SysDept dept = getUserDept();
        if (dept == null || isGroupUser()) {
            return null;
        }
        return String.valueOf(dept.getDeptId());
    }

    /**
     * 组织上限（sys_dept.thrid_dept_id）
     * 集团账号返回 null，表示不加组织限制
     */
    public static String getScopeThridDeptId() {
        return isGroupUser() ? null : getDefaultOrgId();
    }

    /**
     * 把前端传入的组织 id 收敛到权限范围内：
     * 集团账号原样返回；其他账号一律返回本人所属组织，避免越权查询其它单位数据
     */
    public static String clampOrgId(String orgId) {
        return isGroupUser() ? orgId : getDefaultOrgId();
    }
}
