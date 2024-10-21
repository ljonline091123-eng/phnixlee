//文件上传地址
export const uploadFileUrl = process.env.NODE_ENV === "staging"?"/prod-api/file/upload":"/dev-api/file/upload";
//文件服务公用地址
export const offerService = process.env.NODE_ENV === "staging"?"http://192.168.241.29:8001/docs/app":"https://ck.hncig.cn:60023/docs/app";
//文件服务应用名
export const offerRepo = process.env.NODE_ENV === "staging"?"thirdparty-rest1":"zhaocai-prod";
