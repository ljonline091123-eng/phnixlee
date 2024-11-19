/**
 * 调价状态
 * @type {[{label: string, value: number},{label: string, value: number},{label: string, value: number}]}
 */
export const PRICECHANGESTATEOPTIONS = [{
  value: 0,
  label: '调价中'
}, {
  value: 1,
  label: '已调价',
},{
  value: 2,
  label: '放弃调价',
}]

export const PRICETYPEOPTIONS = [{
  label: '固定价',
  value: 1
},{
  label: '浮动价',
  value: 2
}]

/**
 * 价格类型
 * @type {[{label: string, value: string},{label: string, value: string},{label: string, value: string}]}
 */
export const PRICETYPELIST = [{
  label: '固定价',
  value: 1
},{
  label: '浮动价',
  value: 2
},{
  label: '固定浮动价',
  value: 3
}]

export const CONTRACTTYPE = [{
  value: '',
  label: '全部'
}, {
  value: '5',
  label: '劳务分包',
},{
  value: '4',
  label: '专业分包',
},{
  value: '1',
  label: '购买材料',
},{
  value: '2',
  label: '租赁材料',
},{
  value: '3',
  label: '租赁机械（设备）',
},{
  value: '6',
  label: '其他',
}]

