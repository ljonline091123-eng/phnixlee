import request from "@/utils/request";
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询列表
export function listProject(query) {
    return request({
      url: "/business/minProject/getMinProjectListByQuery",
      method: "get",
      params: query,
    });
}