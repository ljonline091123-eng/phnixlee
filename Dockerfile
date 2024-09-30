# 使用官方提供的Nginx镜像作为基础镜像
FROM nginx:1.20.0
# 设置工作目录
RUN mkdir -p /data/nginx/html/zhaocai
WORKDIR /data/nginx/html
# 将本地的dist文件夹复制到容器的/app目录下
COPY ./dist /data/nginx/html/zhaocai
# 移除默认的Nginx配置文件
RUN rm /etc/nginx/conf.d/default.conf
# 添加自定义的Nginx配置文件，用于转发请求到Kubernetes服务
COPY nginx.conf /etc/nginx/conf.d/
# 运行Nginx并且保持在前台运行
CMD ["nginx", "-g", "daemon off;"]
