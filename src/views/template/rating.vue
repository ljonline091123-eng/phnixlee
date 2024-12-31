<template>
  <div>
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
      @submit.native.prevent
    >
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="small"
          @click="handleQuery"
          >查询</el-button
        >
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery"
          >重置</el-button
        >
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <!-- <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['template:rating:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-edit"
          size="mini"
          :disabled="!selectTemplateData.id? true : false"
          @click="handleUpdate"
          v-hasPermi="['template:rating:update']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="!selectTemplateData.id? true : false"
          @click="handleDelete"
          v-hasPermi="['template:rating:remove']"
        >删除</el-button>
      </el-col> -->
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-edit"
          size="small"
          :disabled="!selectTemplateData.id ? true : false"
          @click="handleUpdate"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="!selectTemplateData.id ? true : false"
          @click="handleDelete"
          >删除</el-button
        >
      </el-col>
    </el-row>

    <el-table
      v-loading="ratingLoading"
      :data="ratingList"
      @row-click="selectTemplate"
      stripe
      border
    >
      <el-table-column label="" width="30" align="center">
        <template slot-scope="scope">
          <el-radio v-model="selectTemplateData.id" :label="scope.row.id" />
        </template>
      </el-table-column>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column
        prop="name"
        label="模板名称"
        min-width="300"
        show-overflow-tooltip
      >
        <template slot-scope="{ row }">
          <a
            href="javascript:;"
            class="link-type"
            @click="handleCheck(row.id)"
            >{{ row.name }}</a
          >
        </template>
      </el-table-column>
      <el-table-column
        prop="useUnitName"
        label="使用单位"
        min-width="200"
        show-overflow-tooltip
      ></el-table-column>
      <el-table-column
        prop="createUser"
        align="center"
        label="维护人"
        width="150"
      ></el-table-column>
      <el-table-column
        prop="createTime"
        align="center"
        label="创建日期"
        width="200"
      ></el-table-column>
      <el-table-column prop="state" align="center" label="使用状态" width="150">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.state"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <!-- <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
      <template slot-scope="scope">
        <el-button
          size="mini"
          type="text"
          icon="el-icon-edit"
          @click="handleCheck(scope.row)"
          v-hasPermi="['template:rating:check']"
        >查看</el-button>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-edit"
          @click="handleUpdate(scope.row)"
          v-hasPermi="['template:rating:edit']"
        >修改</el-button>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDelete(scope.row)"
          v-hasPermi="['template:rating:remove']"
        >删除</el-button>
      </template>
    </el-table-column> -->
    </el-table>

    <pagination
      v-show="total > 0"
      @pagination="getList"
      :total="total"
      :page.sync="queryParams.pageNumber"
      :limit.sync="queryParams.pageSize"
    />
    <!-- 详细 -->
    <el-drawer
      :title="title"
      :visible.sync="openView"
      size="80%"
      direction="rtl"
      @close="handleClose"
    >
      <div class="form-body">
        <el-form
          :model="formData"
          ref="form"
          label-position="right"
          label-width="110px"
          size="medium"
          @submit.native.prevent
        >
          <PageTitle title="基本信息" marginBottom="15px" />
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="模板名称"
                prop="name"
                class="required label-right-align"
              >
                <template slot-scope>
                  {{ formData.name }}
                </template>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="评分类型" prop="selectedTypes">
                <el-checkbox-group v-model="formData.selectedTypes">
                  <el-checkbox
                    v-for="dict in dict.type.mark_item_type"
                    :label="dict.value"
                    :key="dict.value"
                    disabled
                    >{{ dict.label }}</el-checkbox
                  >
                </el-checkbox-group>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell" prop="createUser">
              <el-form-item
                label="维护人"
                prop="projectCode"
                class="required label-right-align"
              >
                <template slot-scope>
                  {{ formData.createUser }}
                </template>
              </el-form-item>
            </el-col>
            <!-- <el-col :span="8" class="grid-cell">
              <el-form-item label="使用单位" prop="useUnit">
                <template slot-scope>
                  {{formData.useUnitName}}
                </template>
              </el-form-item>
            </el-col> -->
          </el-row>
          <PageTitle title="评分模板内容" marginBottom="15px" />
          <div
            v-for="(table, index) in formData.biddingMarkCategoryVOList"
            :key="index"
            class="table-section"
          >
            <el-row :gutter="40">
              <el-col :span="10" class="grid-cell">
                <el-form-item
                  label="评分项类型"
                  class="required label-right-align"
                >
                  <template slot-scope>
                    {{ table.itemTypeName }}
                  </template>
                </el-form-item>
              </el-col>
              <el-col :span="10" class="grid-cell">
                <el-form-item label="总分">
                  <template slot-scope>
                    {{ table.totalScore }}
                  </template>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="40">
              <el-col :span="20" class="grid-cell">
                <el-form-item label="评分项" class="required label-right-align">
                  <el-table
                    :data="table.biddingMarkItemVOList"
                    default-expand-all
                    row-key="id"
                    stripe
                    border
                    :tree-props="{ children: 'subBiddingMarkItemDetailVOList' }"
                  >
                    <el-table-column prop="name" label="评分项名称" />
                    <el-table-column
                      prop="highRange"
                      align="center"
                      label="评分项"
                    />
                    <el-table-column
                      prop="contant"
                      align="center"
                      label="评分描述"
                    />
                  </el-table>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-form>
      </div>

      <div slot="footer" class="drawer-footer">
        <el-button @click="openView = false">关闭</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import {
  listRating,
  getRating,
  delRating,
  updateStatus,
  getFanList,
} from "@/api/template/rating";
import PageTitle from "@/components/PageTitle/index";
export default {
  name: "Rating",
  dicts: ["mark_item_type"],
  data() {
    return {
      ratingList: [],
      contractList: [],
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        templateName: undefined,
      },
      formData: {},
      tables: [],
      scoreTypes: ["技术评分", "商务评分"],
      units: [
        { value: "unit1", label: "单位一" },
        { value: "unit2", label: "单位二" },
        { value: "unit3", label: "单位三" },
      ],
      ratingLoading: false,
      multiple: true,
      openView: false,
      title: "",
      selectTemplateData: {},
    };
  },
  created() {
    this.getList();
  },
  components: {
    PageTitle,
  },
  methods: {
    /** 查询采购计划列表 */
    async getList() {
      this.ratingLoading = true;
      try {
        const query = { ...this.queryParams };
        const res = await getFanList(query);
        this.ratingLoading = false;
        this.ratingList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        this.ratingLoading = false;
        console.log(err);
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 任务状态修改
    handleStatusChange(row) {
      let text = row.state === 1 ? "启用" : "停用";
      this.$modal
        .confirm('确认要"' + text + '""' + row.name + '"模板吗？')
        .then(function () {
          return updateStatus(row.id, row.state);
        })
        .then(() => {
          this.$modal.msgSuccess(text + "成功");
        })
        .catch(function () {
          row.state = row.state === 0 ? 1 : 0;
        });
    },
    /** 新增按钮操作 */
    async handleAdd() {
      this.$router.push(`/template/add-rating`);
    },
    /** 查看按钮操作 */
    handleCheck(id) {
      getRating({ id }).then((res) => {
        res.data.selectedTypes = res.data.markCategoryDatailVOList.map((obj) =>
          String(obj.itemType)
        );
        res.data.useUnit = this.units[0].value;
        res.data.useUnitName = this.units[0].label;
        res.data.biddingMarkCategoryVOList =
          res.data.markCategoryDatailVOList.map((obj, index) => {
            res.data.markCategoryDatailVOList[index].itemType = String(
              obj.itemType
            );
            const itemTypeFind = this.dict.type.mark_item_type.find(
              (item) => item.value === String(obj.itemType)
            );
            res.data.markCategoryDatailVOList[index].itemTypeName =
              itemTypeFind.label;
            res.data.markCategoryDatailVOList[index].biddingMarkItemVOList =
              obj.markItemDetailVOList;
            return obj;
          });
        this.formData = res.data;
        console.log(this.formData, "this.formData-this.formData-this.formData");
        this.openView = true;
        this.title = "查看评分模板";
      });
    },
    /** 修改按钮操作 */
    handleUpdate() {
      const { id } = this.selectTemplateData;
      let param = Base64.encode(JSON.stringify(id));
      this.$router.push(`/template/edit-rating/${param}`);
    },
    /** 删除按钮操作 */
    handleDelete() {
      const { id, name } = this.selectTemplateData;
      this.$modal
        .confirm('是否确认删除名称为"' + name + '"的评分模板？')
        .then(function () {
          return delRating(id);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    handleClose() {
      console.log("已关闭");
    },
    /** 选择模板 */
    selectTemplate(row) {
      console.log(row, "aaaa");
      this.selectTemplateData = { id: row.id, name: row.name };
    },
  },
};
</script>

<style lang="scss" scoped>
// .drawer-rating {
//   width: 100%;
//   padding: 16px 16px 16px 16px;
//   .page-title {
//   width: 100%;
//   border-bottom: solid 1px #ccc;
//   padding: 10px;
//   position: relative;
//   display: flex;
//   justify-content: space-between;
//   align-items: center;

//   &::before {
//     content: "";
//     height: 20px;
//     width: 5px;
//     background-color: rgba(41, 65, 137, 1);
//     position: absolute;
//     left: 0;
//     top: 50%;
//     transform: translateY(-50%);
//   }
// }
// }
.subItems {
  margin-left: 55px;
}

.form-body {
  padding: 20px;
}
</style>
