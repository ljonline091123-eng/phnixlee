<template>
  <div class="root">
    <div class="common-title">
      <div class="box">
        <div class="prefixDiv"></div>
        <slot></slot>
      </div>
      <slot name="right"></slot>
    </div>
    <el-divider></el-divider>
  </div>

</template>

<script>
export default {}
</script>

<style lang="scss" scoped>
 .root {
   .el-divider--horizontal {
     margin: 12px 0;
   }
   .common-title {
     display: flex;
     justify-content: space-between;
     width: 100%;
     margin-bottom: 4px;
       font-size: 13px; /* 修改字体大小 */
  font-weight: bolder; /* 修改字体粗细 */
  color: #121735;

     .box {
       display:flex;
       align-items: center
     }

     .prefixDiv {
       margin-right: 4px;
      width: 3px;
    height: 14px;
       background: #2B4ACB;
     }
   }
}

</style>
