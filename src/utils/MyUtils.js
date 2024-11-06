import Vue from 'vue'
const vm = new Vue();
export const showSecretRelatedTips = (func) =>{
  vm.$confirm('禁止上传涉密文件！', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    func();
  }).catch(() => {
    vm.$message({
      type: 'info',
      message: '已取消'
    });
  });
}
