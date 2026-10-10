# =========================================================
# Gaia Backend（后台管理）镜像
# =========================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/gaia-backend.jar /app/app.jar

ENV JAVA_OPTS="\
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.math=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
-XX:+UseG1GC -Xms256m -Xmx512m"

EXPOSE 8088

ENV SPRING_PROFILES_ACTIVE=test

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
