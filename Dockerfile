# 使用官方的 Maven 镜像作为构建阶段的基础镜像
FROM maven:3.8.5-openjdk-17-slim AS builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

EXPOSE 8101

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/network-manager-0.0.1.jar"]
