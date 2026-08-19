# syntax=docker/dockerfile:1

# =====================================================================
# Etapa 1: build -- compila el proyecto con Maven Wrapper (mvnw) sobre
# Java 17 y genera el jar ejecutable (spring-boot-maven-plugin ya lo
# empaqueta con el servidor embebido, ver pom.xml).
#
# Los tests se saltan a proposito en esta etapa (-DskipTests): varios
# (ExcelServiceTest, SecurityConfigIntegrationTest,
# LicitacionesBackendApplicationTests, UsuarioServiceTest, etc.) requieren
# una base de datos MySQL real corriendo en 127.0.0.1, que no existe
# dentro del contenedor de build. Correr la suite completa sigue siendo
# responsabilidad de CI/desarrollo local (./mvnw test), no de esta imagen.
# =====================================================================
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Primero solo lo necesario para resolver dependencias: si el codigo
# fuente cambia pero pom.xml no, Docker reutiliza esta capa cacheada.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Ahora si, el codigo fuente y la compilacion.
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# =====================================================================
# Etapa 2: runtime -- imagen final, solo JRE (no JDK/Maven) para
# produccion: mas liviana y con menos superficie de ataque.
# =====================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuario sin privilegios: el proceso de la aplicacion no corre como root.
# IMPORTANTE: WORKDIR /app se creo con root (linea de arriba, antes de
# este RUN), asi que sin este chown, /app queda root:root y el usuario
# "spring" NO tiene permiso de escritura ahi -- ExcelService.generarExcel
# (Files.createDirectories sobre app.excel.output-dir, por defecto
# "./generated/excel", relativo a /app) fallaria con
# AccessDeniedException("/app/./generated") al intentar crear esa carpeta
# en tiempo de ejecucion. El chown le da a "spring" permiso de escritura
# sobre /app para que pueda crear esa carpeta el mismo.
RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && chown spring:spring /app
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# Puramente informativo (Render no usa EXPOSE para enrutar): el puerto
# real en tiempo de ejecucion lo define la variable de entorno PORT que
# Render inyecta, leida por Spring Boot via server.port=${PORT:8080}
# (ver application.properties). En local, sin PORT definida, cae en 8080.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
