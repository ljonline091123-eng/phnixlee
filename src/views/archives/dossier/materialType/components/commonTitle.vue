<template>
  <div class="module">
   <div class="titleModule">
     <div>
       <slot name="name"></slot>
       <slot name="more" v-if="type">
         <span class="more" @click="clickMoreBtnHandle"></span>
       </slot>
     </div>
   </div>
   <div class="right">
     <slot name="right"></slot>
   </div>
 </div>
</template>

<script>
export default {
 props: {
   type: {
     type: String,
     required: "",
   },
 },
 methods: {
   clickMoreBtnHandle() {
     this.$router.push({
       path: "/portal/" + this.type,
     });
   },
 },
};
</script>

<style  lang="scss" scoped>
.module {
 display: flex;
 justify-content: space-between;
 align-items: center;
  border-bottom: solid 1px #E7E7E7;
 .titleModule {
   height: 45px;
   color: #333333;
   display: flex;
   align-items: center;
   font-size: 18px;
   font-family: Source Han Sans CN-Medium, Source Han Sans CN;
   font-weight: 500;
   font-size: 16px;
   white-space: nowrap;
   &::before {
     content: "";
     width: 4px;
     height: 20px;
     background-color: #2b4acb;
     margin-left: 12px;
     margin-right: 5px;
   }
   div {
     display: flex;
     align-items: center;
     .more {
       font-size: 12px;
       font-weight: unset;
       color: #8b8b8b;
       cursor: pointer;
       margin-left: 5px;
       margin-top: 7px;
       &:active {
         color: #004ea2;
       }
     }
   }
 }
 .right {
   .el-button {
     /* border: 0;
     font-size: 14px;
     padding: 3px 7px; */
     color: #333333;
     margin-right: 8px;
     border-radius: 0;
     font-weight: 400;
     position: relative;
     /* &::after {
       content: " ";
       position: absolute;
       height: 14px;
       width: 1px;
       background: #e6e6e6;
       margin-left: 9px;
     } */
    /* .num {
       color: #ff3a2f;
     } */
   }
 }
}
</style>
