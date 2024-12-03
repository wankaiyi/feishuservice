# 该镜像需要依赖的基础镜像
FROM openjdk:17-jdk-alpine

ARG APOLLO_SECRET_KEY
ENV APOLLO_SECRET_KEY=${APOLLO_SECRET_KEY}
# 将当前目录下的jar包复制到docker容器的/目录下
ADD ./target/feishuservice-1.0.jar /app.jar
# 声明服务运行在9090端口
EXPOSE 9090
# 指定docker容器启动时运行jar包
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-Dserver.shutdown=graceful", "-Dspring.lifecycle.timeout-per-shutdown-phase=10s", "-jar","/app.jar"]