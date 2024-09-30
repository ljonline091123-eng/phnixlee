 /**
 * 自定义指令-千分位
 */
 function formatNumber(value) {
  if (typeof value !== 'number') {
    return value; // 处理非数字情况
  }
  // 使用正则表达式将数字转换为千分位格式
  return formatNumberWithThousandsSeparator(value);
}

function formatNumberWithThousandsSeparator(number) {
  // 将数字转为字符串
  let [integerPart, decimalPart] = number.toString().split('.');

  // 使用正则表达式在整数部分插入千分位分隔符
  let formattedIntegerPart = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

  // 如果有小数部分，将其附加到格式化后的整数部分
  return decimalPart !== undefined ? `${formattedIntegerPart}.${decimalPart}` : formattedIntegerPart;
}

export default {
  // 当被绑定的元素挂载到 DOM 中时
  bind(el, binding) {
    el.textContent = formatNumber(binding.value);
  },
  // 当组件更新时
  update(el, binding) {
    el.textContent = formatNumber(binding.value);
  }
};