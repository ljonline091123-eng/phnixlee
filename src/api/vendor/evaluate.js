import request from '@/utils/request'

export function getlist(query) {
  return request({
    url: "/business/vendor/evaluate/list",
    method: "get",
    params: query,
  });
}

export function save(data) {
  return request({
    url: "/business/vendor/evaluate/save",
    method: "post",
    data: data,
  });
}

export function submit(data) {
  return request({
    url: "/business/vendor/evaluate/submit",
    method: "post",
    data: data,
  });
}

export function addBean() {
  return request({
    url: "/business/vendor/evaluate/add",
    method: "get",
  });
}

export function getById(id) {
  return request({
    url: "/business/vendor/evaluate/getById",
    method: "get",
    params: {
      id,
    },
  });
}

export function deleteById(id) {
  return request({
    url: "/business/vendor/evaluate/deleteById",
    method: "post",
    params: {
      id,
    },
  });
}


