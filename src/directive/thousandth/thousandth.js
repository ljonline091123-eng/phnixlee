 /**
 * 自定义指令-千分位
 */
 export default {
    inserted: function(el) {
      // 获取input节点
      if (el.tagName.toLocaleUpperCase() !== "INPUT") {
        el = el.getElementsByTagName("input")[0];
      }
      console.log(el.value,'------------------------------');
      
      // 千分位
      el.value = isNumber(el.value) && countDecimalPlaces(el.value) <= 4? (el.value && parseFloat(el.value).toLocaleString("zh", {
        minimumFractionDigits: digit(),
        maximumFractionDigits: 4
      })) : el.value || ''
      // 聚焦转化为数字格式（去除千分位）
      el.onfocus = e => {
        let a = el.value && el.value.replace(/,/g, "") || ''; //去除千分号的','
        console.log(el.value, isNumber(el.value),'isNumber(el.value)---------------');
        el.value = isNumber(a) && countDecimalPlaces(a) <= 4? parseFloat(a) : el.value || ''
      };
      el.onblur = e => {
        el.value = isNumber(el.value) && countDecimalPlaces(el.value) <= 4? (el.value && parseFloat(el.value).toLocaleString("zh", {
          minimumFractionDigits: digit(),
          maximumFractionDigits: 4
        })) : el.value || ''
      };
      function countDecimalPlaces(num) {
        // 将数字转换为字符串
        const numStr = num.toString();
        // 查找小数点的位置
        const decimalIndex = numStr.indexOf('.');
        // 如果小数点存在，计算小数位数
        if (decimalIndex !== -1) {
            return numStr.length - decimalIndex - 1;
        }
        // 如果没有小数点，返回 0
        return 0;
      }
      function digit(){
        let count;
        if(countDecimalPlaces(el.value) <= 2){
          count = 2
        }else if(countDecimalPlaces(el.value) >= 4){
          count = 4
        }else{
          count = 3
        }
        return count
      }
      // 判断是否为整数或小数
      function isNumber(str) {
        const numberPattern = /^(?:[1-9]\d*|0)(\.\d+)?$/;
        return numberPattern.test(str);
      }
    }
}