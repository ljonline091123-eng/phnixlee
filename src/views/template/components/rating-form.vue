<template>
  <div class="app-container">
    <el-form :model="formData" ref="form" :rules="rules" label-position="right" label-width="110px" size="medium"
      @submit.native.prevent>
      <div class="page-title">
        <span>基本信息</span>
        <div class="page-title-right">
          <el-button type="primary" plain size="mini" :disabled="isSubmit" @click="$tab.closePage()">取消</el-button>
          <el-button type="primary" size="mini" @click="submitForm('form')" :disabled="isSubmit" :loading="isSubmit">{{ isSubmit? '提交中...' : '确定' }}</el-button>
        </div>
      </div>
      <div class="form-body">
        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell">
            <el-form-item label="项目编号" prop="projectCode" class="required label-right-align">
              <el-input type="text" clearable :readonly="true" disabled v-model="formData.projectCode" />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label=" 项目名称" prop="projectName" class="required label-right-align">
              <el-input v-model="formData.projectName" disabled type="text" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell">
            <el-form-item label="计划编号" prop="procurementPlanCode" class="required label-right-align">
              <el-input type="text" clearable :readonly="true" disabled v-model="formData.procurementPlanCode"
                placeholder="系统自动为您生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label=" 计划名称" prop="procurementPlanName" class="required label-right-align">
              <el-input v-model="formData.procurementPlanName" type="text" clearable :disabled="isSubmit"/>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label="  采购层级" prop="projectHierarchy" class="required label-right-align">
              <el-input v-model="formData.projectHierarchy" type="text" disabled clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label="计划开始时间" prop="beginDate" class="required label-right-align">
              <el-date-picker v-model="formData.beginDate" type="date" style="width:100%" placeholder="选择日期" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label="计划完成时间" prop="endDate" class="required label-right-align">
              <el-date-picker v-model="formData.endDate" type="date" style="width:100%" placeholder="选择日期" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label="计划进场时间" prop="arrivalDate" class="required label-right-align">
              <el-date-picker v-model="formData.arrivalDate" type="date" style="width:100%" placeholder="选择日期" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="8" class="grid-cell">
            <el-form-item label="填报人" prop="procurementReporterName" class="required label-right-align">
              <el-input v-model="formData.procurementReporterName" :disabled="isSubmit" type="text" clearable/>
            </el-form-item>
          </el-col> -->
          <el-col :span="8" class="grid-cell">
            <el-form-item label="采购经办人" prop="procurementOfficerName" class="required label-right-align">
              <el-select v-model="formData.procurementOfficerName" placeholder="请选择" @change="changeOperator">
                <el-option
                  v-for="item in operatorList"
                  :key="item.officerName"
                  :label="item.officerName"
                  :value="item.officerId">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>
      <div class="page-title">
        <span>物资采购类</span>
        <el-button type="success" size="mini" :disabled="isSubmit" @click="splitVisible = true">合约拆分</el-button>
      </div>

      <el-table v-loading="loading" :data="planList" border size="mini" default-expand-all>
        <el-table-column type="expand" v-if="planList[0].children && planList[0].children.length">
          <template slot-scope="props">
            <el-table :data="props.row.children" size="mini" stripe @selection-change="handleSelectionChange">
              <el-table-column type="selection"></el-table-column>
              <el-table-column label="拆分合约规划名称" align="center" prop="splitContractName" width="200">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.splitContractName" :disabled="isSubmit"/>
                </template>
              </el-table-column>
              <el-table-column label="拟签约合同承包范围" align="center" prop="contractScope" width="200">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.contractScope" :disabled="isSubmit"/>
                </template>
              </el-table-column>
              <el-table-column label="清单" align="center" props="inventory">
                <template slot-scope="inventory">
                  <el-table size="medium" :data="inventory.row.children">
                    <el-table-column label="序号" type="index" width="50" align="center" />
                    <el-table-column label="清单编码" width="200" align="center" prop="materialsCode" />
                    <el-table-column label="清单名称" width="200" align="center" prop="materialsName" />
<!--                    <el-table-column label="规格型号" align="center" prop="specification" />-->
                    <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
                    <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules" />
                    <el-table-column label="工作内容" align="center" prop="workContent" />
                    <el-table-column label="计量单位" align="center" prop="unitMeasurement" />
                    <el-table-column label="清单数量" align="center" prop="count"/>
                    <el-table-column label="单价（含税）" align="center" prop="priceIncludingTax" />
                    <el-table-column
                      align="center"
                      label="操作"
                      width="50">
                      <template slot-scope="{row,$index}">
                        <el-button type="text" size="small" @click="delHandle(row,$index,inventory.row.index)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="合约规划名称" width="200" align="center" prop="contractName" />
        <el-table-column label="规划金额（含税）" align="center" prop="totalAmount" />
        <el-table-column label="已发生规划金额" align="center" prop="incurredAmount" />
        <el-table-column label="规划余量" align="center" prop="surplusAmount" />
        <el-table-column label="拟定招标方式" align="center" prop="contractTypeName" />
        <el-table-column label="清单" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-view" @click="handelInventory(scope.row)">查看清单</el-button>
          </template>
        </el-table-column>

      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNumber" :limit.sync="queryParams.pageSize"
        @pagination="getList" />

    </el-form>

    <!-- 选择项目合约规划 -->
    <el-dialog title="清单" :visible.sync="inventoryVisible" width="70%">
      <el-table v-loading="loading" :data="inventoryList" stripe>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="清单编码" width="200" align="center" prop="materialsCode" />
        <el-table-column label="清单名称" width="200" align="center" prop="materialsName" />
<!--        <el-table-column label="规格型号" align="center" prop="specification" />-->
        <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
        <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules" />
        <el-table-column label="工作内容" align="center" prop="workContent" />
        <el-table-column label="计量单位" align="center" prop="unitMeasurement" />
        <el-table-column label="测算数量" align="center" prop="count" />
        <el-table-column label="已使用数量" align="center" prop="usedCount" />
        <el-table-column label="基准价" align="center" prop="basePrice" />
        <el-table-column label="浮动值" align="center" prop="floatingValue" />
        <el-table-column label="税率" align="center" prop="taxRate" />
        <el-table-column label="单价（含税）" align="center" prop="priceIncludingTax" />
      </el-table>
    </el-dialog>

    <!-- 合约拆分弹出层 -->
    <el-dialog title="合约拆分" :visible.sync="splitVisible" width="20%" @closed="splitColsed">
      <el-form :model="splitForm" :rules="splitRules" ref="splitFormRef">
        <el-form-item label="拟拆分合同份数：" prop="num" label-width="135px">
          <el-input v-model.number="splitForm.num" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="splitVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSplit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Base64 } from 'js-base64';
import { getContractMaterials, saveProcurementPlan, getListProcurementOfficer } from '@/api/procurement/plan'
export default {
  name: "add-plan",
  dicts: ['plan_type'],
  data() {
    let checkNum = (rule, value, callback) => {
      if (!/^[1-9]\d*$/.test(value)) {
        callback(new Error('请输入正整数'));
      } else {
        callback();
      }
    }
    return {
      formData: {
        projectHierarchy:'项目'
      }, //form表单数据
      planList: [],
      inventoryList: [],
      rules: {
        procurementPlanName: [{
          required: true,
          message: '计划名称不可为空',
        }],
        beginDate: [{
          required: true,
          message: '计划开始时间不能为空',
        }],
        endDate: [{
          required: true,
          message: '计划完成时间不能为空',
        }],
        arrivalDate: [{
          required: true,
          message: '计划进场时间不能为空',
        }],
        procurementReporterName: [{
          required: true,
          message: '填报人不能为空',
        }],
        procurementOfficerName: [{
          required: true,
          message: '采购经办人不能为空',
        }],
      },
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        procurementPlanCode: undefined,
        procurementPlanName: undefined,
        projectName: undefined,
        operator: undefined,
        procurementPlanType: 'all'
      },
      inventoryVisible: false,
      splitVisible: false, //是否显示拆分合同
      splitForm: {
        num: ''
      }, //拆分合同数表单
      splitRules: {
        num: [
          { required: true, message: '请输入拆分的份数', trigger: 'blur' },
          { validator: checkNum, trigger: 'blur' }
        ],
      },
      currentContract: {},
      isSubmit: false,
      indexs:[],
      operatorList:[]
    };
  },
  created() {
    // this.getList();
    this.getListProcurementOfficer()
  },
  methods: {
    //提交
    submitForm(formName) {
      console.log(this.planList,'ppp');
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid,done) => {
        if (valid) {
          const { indexs } = this
          if(!indexs.length){
            this.isSubmit = false;
            this.$message({
              message: '拆分合约不能为空',
              type: 'error'
            });
            return false;
          }
          const loading = this.$loading({
            lock: true,
            text: '数据提交中...',
            background: 'rgba(0, 0, 0, 0.7)'
          });
          const { procurementPlanName, beginDate, endDate, arrivalDate, procurementReporter  = '123456', procurementReporterName, procurementOfficer, procurementOfficerName } = this.formData;
          const { contractName,contractCode, contractType:procurementPlanType, contractTypeName,totalAmount, incurredAmount,surplusAmount,biddingType  } = this.currentContract;
          const splitRequestList = this.planList[0].children.filter(item => indexs.includes(item.index)).map(item => {
            return {
              splitContractName:item.splitContractName,
              contractScope:item.contractScope,
              materialsLists:item.children.map(child => ({
                ...child
              }))
            }
          })
          const contractPlanning = {
            contractPlanningCode:contractCode,
            contractPlanningName:contractName,
            contractPlanningCategory:contractTypeName,
            plannedAmountInclTax: totalAmount,
            incurredPlannedAmount: incurredAmount,
            planningBalance: surplusAmount,
            biddingMethod:biddingType
          }
          const formData = {
            procurementPlan:{
              projectHierarchy:'项目',contractName,contractCode, procurementPlanType,
              procurementPlanName, beginDate, endDate, arrivalDate, procurementReporter, procurementReporterName, procurementOfficer, procurementOfficerName,
            },
            splitRequestList,
            contractPlanning
          }
          console.log(formData,'this.formData');
          try{
            const res = await saveProcurementPlan(formData);
            loading.close();
            this.$message({
              message: '保存成功',
              type: 'success'
            });
            console.log(res,'r~~~~~~~~~~~~~~~~~');
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              let param = Base64.encode(JSON.stringify(res.data))
              this.$router.push(`/procurement/plan-detail/${param}`);
            })
          }catch(err){
            console.log(err);
            this.isSubmit = false;
            loading.close();
            done()
          }
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },
    //切换tab类型
    handleTypeClick(tab) {
      this.queryParams.procurementPlanType = tab.name;
    },
    /** 查询定时任务列表 */
    getList() {
      this.loading = false;

    },
    /** 搜索按钮操作 */
    handleQuery() {
      console.log(this.queryParams, 'this.queryParams');
      this.queryParams.pageNumber = 1;
      // this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$router.push("");
    },
    //查看清单
    handelInventory(id) {
      this.inventoryVisible = true;
    },
    // 拆分合同
    handleSplit() {
      this.$refs['splitFormRef'].validate((valid) => {
        if (valid) {
          const { num } = this.splitForm;
          const children = []
          Array.from({ length: num }).forEach((_, index) => {
            children.push({
              index,
              children: this.inventoryList.map(item => ({
                materialsCode: item.materialsCode,
                materialsName: item.materialsName,
                specification: item.specification,
                unitMeasurement: item.unitMeasurement,
                count: item.count,
                priceIncludingTax:item.priceIncludingTax
              })),
            })
          });
          this.$set(this.planList[0], 'children', children);
          this.splitVisible = false;
        } else {
          return false;
        }
      });
    },
    splitColsed() {
      this.$refs['splitFormRef'].resetFields();
    },
    submit(){
      console.log('sub');
    },
    handleSelectionChange(selection) {
      this.indexs = selection.map(item => item.index);
      console.log(this.indexs, 'this.indexs');
    },
    delHandle(row,i,index){
      console.log(this.planList[0].children[index].children,'this.planList[0].children[index]');
      this.planList[0].children[index].children.splice(i,1);
    },
    async getListProcurementOfficer(){
      try{
        const res = await getListProcurementOfficer()
        this.operatorList = res.data;
        console.log(res,'采购经办人');
      }catch(err){
        console.log(err);
      }
    },
    changeOperator(val){
      this.formData.procurementOfficer = val;
      this.formData.procurementOfficerName = this.operatorList.find(item => item.officerId === val).officerName
    }
  },
  watch: {
    '$route.params.params': {
      async handler(val) {
        if (val) {
          const param = JSON.parse(Base64.decode(val))
          console.log(param,'param--param--param');
          this.currentContract = param;
          this.planList.push(param)
          const { contractCode } = this.currentContract
          const res = await getContractMaterials(contractCode)
          this.inventoryList = res.data;
          this.formData.projectName = param.contractName;
          this.formData.projectCode = param.contractCode;
        }
      },
      immediate: true
    },
  }
};
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.form-body {
  padding: 20px;
}
</style>
