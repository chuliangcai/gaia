# =========================================================
# Gaia Server（Dubbo Provider）镜像
# 基于 eclipse-temurin:17-jre（已带 JDK 17 运行时 + add-opens 推荐参数可通过 JAVA_OPTS 注入）
# =========================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制 Spring Boot fat jar（重命名为 app.jar 以简化启动命令）
COPY target/gaia-server.jar /app/app.jar

# 容器内 JVM 参数：JDK 17 反射限制 + 较大堆
ENV JAVA_OPTS="\
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.math=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
-XX:+UseG1GC -Xms512m -Xmx1g"

EXPOSE 20881 8081

# 默认走 test profile（K8s 注册中心），可被 K8s env.SPRING_PROFILES_ACTIVE 覆盖
ENV SPRING_PROFILES_ACTIVE=test

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
