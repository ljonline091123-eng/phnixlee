<template>
    <div class="left">
      <!-- <div class="custom-tree-node" style="height: 30px;align-items: center;margin:3px 0 0 10px;">
        <span style="font-weight: 500;color: #303133;">{{title}}</span>
        </div> -->
        <div v-if="queryType" style="   width: 100%;height: 33px;margin: -10px 10px 16px 0; background-color: #ffffff;">
          <el-radio-group
          style="margin:0px 0 0 10px;"
          v-model="queryRadioType"
          size="small"
          @change="handleRadioChange"
        >
            <el-radio-button
            :label="dict.value"
            :name="dict.value" 
            v-for="dict in radioList"
            :key="dict.value"
            >{{ dict.label }}</el-radio-button>
        </el-radio-group>
        </div>
        <el-input
        style="width: 220px;margin-left: 10px;"
            v-model="label"
            placeholder="请输入名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
          />
        <div class="circle-buttons" style="margin: 10px 0 0 10px">
          <button
            v-for="n in buttonCount"
            :key="n"
            class="circle-btn"
            :class="levelExpand==n?'circle-btn-selected':'circle-btn'"
            @click="expandNodes(n)"
          >
            {{ n }}
          </button>
        </div>
        <el-tree
          v-loading="loading"
          :data="deptOptions"
          class="address-tree"
          :props="defaultProps"
          :filter-node-method="filterNode"
          :current-node-key="currentNodeKey"
          :default-expanded-keys="defaultExpandedKeys"
          ref="tree"
          node-key="id"
          highlight-current
          @node-click="handleNodeClick"
        >
        <span slot-scope="{ node, data }" class="custom-tree-node">
          <span class="tooltip" >
            <el-tooltip :content="data.label" placement="top" effect="dark">
            <span :class="node.level>2?'add-f-s':'add-f-s-14'">{{ data.label }}</span>
            </el-tooltip>
          </span>
        </span>
        </el-tree>
        
      </div>
  </template>
  <script>
  import {mapGetters} from "vuex";
  export default {
    name: "treeMenu",
    data() {
      return {
        queryRadioType: "1",
        radioList: [
        {
          value: '1',
          label: "材料类",
        },
        {
          value: '2',
          label: "设备类",
        },
        {
          value: '3',
          label: "劳务类",
        },
        {
          value: '4',
          label: "专业分包类",
        },
      ],
        queryParams: {type:'1'},
        buttonCount: 1,
        // levelExpand:1, //默认展示的级别
        label:undefined,
        modelOptions: [],
        parentNode:{},
        // currentNodeKey: '3897276558322302978', // 默认选中节点1
        value: '2',
        // 部门名称
        deptName: undefined,
        defaultProps: {
        children: "children",
        label: "label",

        nodeCount:0,
        preNodeId:null,
        curNodeId:null,
        nodeTimer:null

      },
      };
    },
    computed:{
    ...mapGetters(['project','org']),
   },
  props: {
 
    // 表格数据
    deptOptions: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
   
   
    
    title: {
      type: String,
      default: "",
    },
    currentNodeKey: {
      type: String,
      default: "",
    },
    defaultExpandedKeys: {
      type: Array,
      default: () => [],
    },
    levelExpand: {
      type: Number,
      default: 1,
    },
     //是否显示查询类型
     queryType: {
      type: Boolean,
      default: false,
    },
},
watch:{
     // 根据名称筛选部门树
     label(val) {
      this.$refs.tree.filter(val);
    },
  
      deptOptions: {
        handler(arr) {
          this.initButtons()
        }
      },
   
  },
    created() {
    },
    mounted() {
   
			this.onRefresh();
		
	},
    methods: {
      handleRadioChange(value) {
        let radio =this.radioList[value]
        console.log(JSON.stringify(value))
        this.queryParams.type=value
        this.$emit("query", this.queryParams);
    },
 
 

    onRefresh() {
        this.$emit("query", this.queryParams);
		},
    // 初始化按钮
    initButtons() {
      const maxLevel = this.calculateMaxLevel(this.deptOptions);
      this.buttonCount = maxLevel + 1;
    },
         // 递归计算最大层级
    calculateMaxLevel(nodes, level = 0) {
      let maxLevel = level;
      nodes.forEach((node) => {
        if (node.children && node.children.length > 0) {
          maxLevel = Math.max(
            maxLevel,
            this.calculateMaxLevel(node.children, level + 1)
          );
        }
      });
      return maxLevel;
    },
       // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
          // 节点单击事件
    handleNodeClick(data,node,prop) {
      console.log(data,node,prop);
      this.nodeCount++
      if( this.preNodeId && this.nodeCount >= 2){
        this.curNodeId = data.id 
        this.nodeCount = 0
        if(this.curNodeId == this.preNodeId){//第一次点击的节点和第二次点击的节点id相同
          this.curNodeId = null
          this.preNodeId = null   
          this.$emit('treeClick', data,node,this.queryParams)   
          return
        }
      }
      this.preNodeId = data.id
      this.nodeTimer = setTimeout(() => { //300ms内没有第二次点击就把第一次点击的清空
        this.preNodeId  = null
        this.nodeCount = 0
      },300)   
    },

    
    expandNodes(level) {
      this.levelExpand=level
      if (level === 1) {
          // 关闭所有默认节点
          const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
          allNodes.forEach((node) => {
              // 关闭所有父节点
              if (node.level === 1) {
                tree.store.getNode(node.key).expanded = false;
              }
          });
        } else if (level === 2) {
          const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }else if (level === 3) {
           const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }else if (level === 4) {
            const tree = this.$refs.tree;
            const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==3){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }
        else if (level === 5) {
            const tree = this.$refs.tree;
            const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==3){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==4){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }
    },
    }
  };
  </script>
  <style lang="scss" scoped>
    .left {
      /* width: 260px; */
      margin:0px 10px 0 10px;
      background-color: #ffffff;
    }
   
    .tooltip {
      width: 100%;
    margin-right: 5px;
    font-size: 13px;
    border-radius: 4px;
    box-sizing: border-box;
    white-space: nowrap;
    padding: 4px;
  }
  .custom-tree-node:hover .add-f-s-14 {
    display: inline-block; /* 或者 block，根据需要 */
    max-width: 140px; /* 确保不超过父容器的宽度 */
    white-space: nowrap; /* 防止文本换行 */
    overflow: hidden; /* 隐藏溢出的内容 */
    text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
    }
  .custom-tree-node:hover .add-f-s {
  display: inline-block; /* 或者 block，根据需要 */
  max-width: 80px; /* 确保不超过父容器的宽度 */
  white-space: nowrap; /* 防止文本换行 */
  overflow: hidden; /* 隐藏溢出的内容 */
  text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
  }
 .custom-tree-node {
  display: flex;
  justify-content: space-between;
 }
::v-deep .vue-treeselect {
  .vue-treeselect__control {
    height: 32px;
  }
}
.address-tree {
  margin: 0;
  height: calc(50vh);
  overflow-y: scroll;

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep .icon-shouyetianchong:before {
    content: "\E692";
    color: #004ea2;
  }

  ::v-deep .icon-24gf-folderOpen:before {
    content: "\eac5";
    color: #004ea2;
  }

  ::v-deep .el-tree-node {
    .el-tree-node__content {
      height: auto;
      padding: 2px 0;
      margin: 2px 0;

      font-size: 13px;
      color: #606266;
      &:hover .hover-operations { 
      display: inline-block;
    }

      .el-tree-node__label {
        white-space: pre-wrap;
        line-height: 20px;
      }
    }
  }
}

  .operation-view, .hover-operations { 
    position: absolute;
    right: 8px;
    display: none;
    padding: 5px 0px;
    margin-left: 50px;
    /* margin-left: 50px; */
    color: #777777;
  }
  .small-operation-btn {
    margin: 0px 3px;
    color: #3D98F9;
  }


::v-deep  .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content  {
  color: #2b4acb !important;
  
  .add-f-s-14 {
    display: inline-block; /* 或者 block，根据需要 */
    max-width: 140px; /* 确保不超过父容器的宽度 */
    white-space: nowrap; /* 防止文本换行 */
    overflow: hidden; /* 隐藏溢出的内容 */
    text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
    }
  .hover-operations { 
      display: inline-block;
    }
}
.circle-btn {
  background-color: #e8e8ef;
  color: #999;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
.circle-btn-selected {
  background-color: #2b4acb;
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
/* 鼠标移上去时的效果 */
.circle-btn:hover {
  background-color: #2b4acb;
  color: #ffffff;
}
::v-deep .left_box{
    background: #ffffff;

}
  </style>
  