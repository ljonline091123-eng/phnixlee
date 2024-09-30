#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20231130.sql ./mysql/db
cp ../sql/ry_config_20231204.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../zhaocai-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy zhaocai-gateway "
cp ../zhaocai-gateway/target/zhaocai-gateway.jar ./ruoyi/gateway/jar

echo "begin copy zhaocai-auth "
cp ../zhaocai-auth/target/zhaocai-auth.jar ./ruoyi/auth/jar

echo "begin copy zhaocai-visual "
cp ../zhaocai-visual/zhaocai-monitor/target/zhaocai-visual-monitor.jar  ./ruoyi/visual/monitor/jar

echo "begin copy zhaocai-modules-system "
cp ../zhaocai-modules/zhaocai-system/target/zhaocai-modules-system.jar ./ruoyi/modules/system/jar

echo "begin copy zhaocai-modules-file "
cp ../zhaocai-modules/zhaocai-file/target/zhaocai-modules-file.jar ./ruoyi/modules/file/jar

echo "begin copy zhaocai-modules-job "
cp ../zhaocai-modules/zhaocai-job/target/zhaocai-modules-job.jar ./ruoyi/modules/job/jar

echo "begin copy zhaocai-modules-gen "
cp ../zhaocai-modules/zhaocai-gen/target/zhaocai-modules-gen.jar ./ruoyi/modules/gen/jar

