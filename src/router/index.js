import Vue from "vue";
import Router from "vue-router";
import store from "@/store";
import { ssoLogin } from "@/api/login";
import {
  setToken,
  getToken,
  setControlToken,
  getZcToken,
  getControlToken,
  removeToken,
  removeControlToken,
} from "@/utils/auth"; // 获取token

Vue.use(Router);

/* Layout */
import Layout from "@/layout";
import appConstant from "@/appConstant";

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect"),
      },
    ],
  },
  {
    path: "/login",
    component: () => import("@/views/login"),
    hidden: true,
  },
  {
    path: "/pageoffice",
    component: () => import("@/views/pageoffice"),
    hidden: true,
  },
  {
    path: "/register",
    component: () => import("@/views/register"),
    hidden: true,
  },
  {
    path: "/404",
    component: () => import("@/views/error/404"),
    hidden: true,
  },
  {
    path: "/401",
    component: () => import("@/views/error/401"),
    hidden: true,
  },
  {
    path: "",
    component: Layout,
    redirect:
      appConstant.platform == "1"
        ? "/procurement/plan"
        : "/evaluate-expert/evaluate-bids",
    // children: [
    //   {
    //     path: 'index',
    //     component: () => import('@/views/index'),
    //     name: 'Index',
    //     meta: { title: '首页', icon: 'dashboard', affix: true }
    //   }
    // ]
  },
  {
    path: "/flowable",
    component: Layout,
    hidden: true,
    permissions: ["flowable:definition:edit"],
    children: [
      {
        path: "definition/model/",
        component: () => import("@/views/flowable/definition/model"),
        name: "Model",
        meta: { title: "model" },
      },
    ],
  },
  {
    path: "/flowable",
    component: Layout,
    hidden: true,
    permissions: ["flowable:task:handle"],
    children: [
      {
        path: "task/record/index",
        component: () => import("@/views/flowable/task/record/index"),
        name: "Record",
        meta: { title: "record" },
      },
    ],
  },
  {
    path: "/user/a",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    children: [
      {
        path: "profile",
        component: () => import("@/views/system/user/profile/index"),
        name: "Profile",
        meta: { title: "个人中心", icon: "user" },
      },
    ],
  },
  {
    path: "/procurement",
    component: Layout,
    hidden: true,
    redirect: [""],
    children: [
      {
        path: "add-plan/:params",
        component: () => import("@/views/procurement/add-plan.vue"),
        name: "add-plan",
        meta: { title: "新增采购计划", activeMenu: "/procurement/plan" },
      },
      {
        path: "plan-detail/:params",
        component: () => import("@/views/procurement/plan-detail.vue"),
        name: "plan-detail",
        meta: { title: "采购计划详情", activeMenu: "/procurement/plan" },
      },
      {
        path: "add-scheme/:params",
        component: () => import("@/views/procurement/add-scheme.vue"),
        name: "add-scheme",
        meta: { title: "新增采购方案", activeMenu: "/procurement/scheme" },
      },
      {
        path: "scheme-detail/:params",
        component: () => import("@/views/procurement/scheme-detail.vue"),
        name: "scheme-detail",
        meta: { title: "采购方案详情", activeMenu: "/procurement/scheme" },
      },
      {
        path: "tendering/:params",
        component: () => import("@/views/procurement/tendering.vue"),
        name: "tendering",
        meta: { title: "招标管理", activeMenu: "/procurement/bindding" },
      },
      {
        path: "add-contract",
        component: () => import("@/views/procurement/add-contract.vue"),
        name: "addContract",
        meta: {
          title: "新增合同信息",
          activeMenu: "/procurement/sign-contract",
        },
      },
      {
        path: "edit-contract",
        component: () => import("@/views/procurement/edit-contract.vue"),
        name: "editContract",
        meta: {
          title: "修改合同信息",
          activeMenu: "/procurement/sign-contract",
        },
      },
      {
        path: "contract-detail/:params",
        component: () => import("@/views/procurement/contract-detail.vue"),
        name: "contractDetail",
        meta: {
          title: "合同信息详情",
          activeMenu: "/procurement/sign-contract",
        },
      },
    ],
  },

  {
    path: "/expert",
    component: Layout,
    hidden: true,
    redirect: [""],
    children: [
      {
        path: "add-expert/:params",
        component: () => import("@/views/expert/add-expert.vue"),
        name: "add-expert",
        meta: { title: "专家信息新增", activeMenu: "/expert/expert" },
      },
      {
        path: "expert-detail/:params",
        component: () => import("@/views/expert/add-expert.vue"),
        name: "expert-detail",
        meta: { title: "专家信息", activeMenu: "/expert/expert-detail" },
      },
    ],
  },
  {
    path: "/evaluate-expert",
    component: Layout,
    hidden: true,
    redirect: [""],
    children: [
      {
        path: "evaluate-bids-detail/:params",
        component: () =>
          import("@/views/evaluate-expert/evaluate-bids-detail.vue"),
        name: "evaluate-bids-detail",
        meta: {
          title: "评标任务详情",
          activeMenu: "/evaluate-expert/evaluate-bids",
        },
      },
      {
        path: "evaluate-bids-end-detail/:params",
        component: () =>
          import("@/views/evaluate-expert/evaluate-bids-end-detail.vue"),
        name: "evaluate-bids-end-detail",
        meta: {
          title: "已完成评标任务详情",
          activeMenu: "/evaluate-expert/evaluate-bids-end",
        },
      },
    ],
  },
  {
    path: "/template",
    component: Layout,
    hidden: true,
    redirect: [""],
    children: [
      {
        path: "rating",
        component: () => import("@/views/template/rating.vue"),
        name: "rating",
        meta: { title: "评分模板", activeMenu: "/template/rating" },
      },
      // {
      //   path: 'rating-detail/:params',
      //   component: () => import('@/views/template/rating-detail.vue'),
      //   name: 'rating-detail',
      //   meta: { title: '评分模板详情', activeMenu: '/template/rating'  }
      // },
      {
        path: "add-rating",
        component: () => import("@/views/template/add-rating.vue"),
        name: "add-rating",
        meta: { title: "新增评分模板", activeMenu: "/template/rating" },
      },
      {
        path: "edit-rating/:params",
        component: () => import("@/views/template/edit-rating.vue"),
        name: "edit-rating",
        meta: { title: "修改评分模板", activeMenu: "/template/rating" },
      },
    ],
  },
  {
    path: "/vendor",
    component: Layout,
    hidden: true,
    redirect: [""],
    children: [
      {
        path: "vendor-detail/:params",
        component: () => import("@/views/vendor/vendor-detail.vue"),
        name: "vendor-detail",
        meta: { title: "供应商详情", activeMenu: "/vendor/base" },
      },
      {
        path: "vendor-record-detail/:params",
        component: () => import("@/views/vendor/vendor-record-detail.vue"),
        name: "vendor-record-detail",
        meta: {
          title: "供应商合作记录详情",
          activeMenu: "/vendor/vendor-record",
        },
      },
    ],
  },
];

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: "/system/user-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:user:edit"],
    children: [
      {
        path: "role/:userId(\\d+)",
        component: () => import("@/views/system/user/authRole"),
        name: "AuthRole",
        meta: { title: "分配角色", activeMenu: "/system/user" },
      },
    ],
  },
  {
    path: "/system/role-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:role:edit"],
    children: [
      {
        path: "user/:roleId(\\d+)",
        component: () => import("@/views/system/role/authUser"),
        name: "AuthUser",
        meta: { title: "分配用户", activeMenu: "/system/role" },
      },
    ],
  },
  {
    path: "/system/dict-data",
    component: Layout,
    hidden: true,
    permissions: ["system:dict:list"],
    children: [
      {
        path: "index/:dictId(\\d+)",
        component: () => import("@/views/system/dict/data"),
        name: "Data",
        meta: { title: "字典数据", activeMenu: "/system/dict" },
      },
    ],
  },
  {
    path: "/monitor/job-log",
    component: Layout,
    hidden: true,
    permissions: ["monitor:job:list"],
    children: [
      {
        path: "index/:jobId(\\d+)",
        component: () => import("@/views/monitor/job/log"),
        name: "JobLog",
        meta: { title: "调度日志", activeMenu: "/monitor/job" },
      },
    ],
  },
  {
    path: "/tool/gen-edit",
    component: Layout,
    hidden: true,
    permissions: ["tool:gen:edit"],
    children: [
      {
        path: "index/:tableId(\\d+)",
        component: () => import("@/views/tool/gen/editTable"),
        name: "GenEdit",
        meta: { title: "genEdit", activeMenu: "/tool/gen" },
      },
    ],
  },
];

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push;
let routerReplace = Router.prototype.replace;
// push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch((err) => err);
};
// replace
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch((err) => err);
};

const router = new Router({
  mode: "history", // 去掉url中的#
  base: "/zhaocai/", // 这里设置为/zhaocai/
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes,
});

router.beforeEach((to, from, next) => {
  console.log(to, from);
  let wujie = "";
  if (
    process.env.NODE_ENV === "staging" ||
    process.env.NODE_ENV === "production"
  ) {
    wujie = window.$wujie?.props.token || "";
    const oldToken = getControlToken();
    // wujie = getZcToken();
    if (wujie !== oldToken) {
      store.commit("SET_TOKEN", "");
      store.commit("SET_ROLES", []);
      store.commit("SET_PERMISSIONS", []);
      removeToken();
      removeControlToken();
    }
    console.log(wujie, "getZcToken");
  } else {
    // wujie = window.$wujie?.props.token || "";
    wujie =
      window.$wujie?.props.token ||
      "Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImUzZWJhZDBjLTk3ZmUtNDgwNC1iZTgxLWE0ZTczZjFiODUwN18xODE2MzY1MTczMyJ9.h3pKJtm6e_f18FQokWcUN0KvbGh4llPL62W9gkBdTlPsw0u6KXy8rfSRXEGz9_fY8ahARUHCLFVcYioPWYEDMQ";
  }
  console.log(window.$wujie?.props, "window.$wujie?.props");

  setControlToken(wujie);
  const token = getToken();
  if (!token) {
    if (wujie) {
      ssoLogin(wujie)
        .then((res) => {
          setToken(res.data.access_token);
          // store.commit('SET_PERMISSIONS', res.data.permissions)
          console.log(to.query.redirect, "to.query.redirect");
          console.log(to.path, "to.path");
          let redirect =
            to.query?.redirect?.replace(/\$/g, "/") ||
            to.path?.replace(/\$/g, "/");
          next(redirect);
        })
        .catch((err) => {
          console.log(err);
          // next();
        });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
