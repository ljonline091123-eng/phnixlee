/**
 * 报表查询公共逻辑(招标率统计 / 供应商报表 / 问题报表 三个页面专用)
 *
 * 为什么不复用 mixins/mixin.js：
 * 1. 日期区间要拆成 startDate/endDate(这三个报表后端 VO 的入参)，老报表用的是 startTime/endTime；
 * 2. 老 mixin 里会调 listDept() 拉全量部门树，这三个报表的组织树改由 ShowTablePro 按登录用户
 *    数据权限加载(/system/dept/getDeptTree，单位+部门)，不再依赖全量部门接口。
 * 修改这里前请先确认后端 VO 里的日期字段名。
 */
export const mixinReport = {
  data() {
    return {
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
      loading: false,
    };
  },
  methods: {
    /**
     * 查询
     * @param params ShowTablePro 抛出的查询参数；分页组件也会走这个方法(只带 {page, limit})
     */
    handleQuery(params) {
      const queryParams = params || {};
      // 拆分日期区间
      queryParams.startDate = queryParams.dateRange && queryParams.dateRange[0];
      queryParams.endDate = queryParams.dateRange && queryParams.dateRange[1];
      this.queryParams = Object.assign({}, this.queryParams, queryParams);
      this.getList(this.queryParams);
    },
    /**
     * 导出
     * @param params
     */
    handleExport(params) {
      const queryParams = Object.assign({}, this.queryParams, params || {});
      queryParams.startDate = queryParams.dateRange && queryParams.dateRange[0];
      queryParams.endDate = queryParams.dateRange && queryParams.dateRange[1];
      this.queryParams = queryParams;
      this.getExport(this.queryParams);
    },
  },
};
