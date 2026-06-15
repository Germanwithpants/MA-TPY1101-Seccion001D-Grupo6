# ConectaTarot — Guía de Instalación de Entorno (Setup Equipo)

## 📱 Aplicación Android

El código fuente de la app móvil nativa (Kotlin/Android Studio) se encuentra en la rama [`app-android`](https://github.com/matiastiznado9/MA-TPY1101-Seccion001D-Grupo6/tree/app-android) de este repositorio.


## Objetivo

Esta guía es únicamente para que cualquier integrante clone el proyecto y pueda dejar **el mismo entorno de trabajo** funcionando que el usado en desarrollo.

No documenta historias del sprint ni pruebas funcionales; solo instalación y configuración.

---

# Requisitos de Software

Instalar exactamente esto:

## 1. Java

Usar:

* Java 21 LTS (recomendado)

Verificar:

```bash
java -version
```

Debe mostrar algo como:

```bash
openjdk version "21"
```

---

## 2. Git

Verificar:

```bash
git --version
```

---

## 3. MySQL Server

Instalar:

* MySQL 8+
* MySQL Workbench

Verificar conexión local:

```text
Host: localhost
Port: 3306
User: root
Password: (tu password)
```

---

## 4. Node.js

Usar versión LTS.

Verificar:

```bash
node -v
npm -v
```

---

## 5. Visual Studio Code

Extensiones recomendadas:

Instalar:

* Extension Pack for Java
* Spring Boot Extension Pack
* Thunder Client
* Angular Language Service
* GitLens (opcional)

---

# Clonar Repositorio

```bash
git clone <url-repo>
cd ConectaTarot
```

---

# Estructura esperada

```bash
ConectaTarot/
├── backend/
├── frontend/
├── database/
├── docs/
└── Gestion/
```

---

# Backend Setup

Entrar:

```bash
cd backend
```

---

## Dependencias Maven

Usamos Maven Wrapper.

Verificar:

Windows:

```powershell
.\mvnw -v
```

Instalar dependencias:

```powershell
.\mvnw clean install
```

---

## application.properties

Ruta:

```bash
backend/src/main/resources/application.properties
```

Configurar:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mi_app
spring.datasource.username=root
spring.datasource.password=root123!

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=conectatarot_secret_key_2026
jwt.expiration=86400000
```

---

# Base de Datos

Ejecutar una vez:

```sql
CREATE DATABASE mi_app;
```

Roles base:

```sql
USE mi_app;

INSERT INTO rol (nombreRol)
VALUES
('ADMIN'),
('CLIENTE'),
('TAROTISTA');
```

---

# Levantar Backend

Desde /backend:

```powershell
.\mvnw spring-boot:run
```

Debe iniciar:

```text
Tomcat started on port 8080
```

Backend:

```text
http://localhost:8080
```

---

# Frontend Setup

Entrar:

```bash
cd frontend
```

Instalar paquetes:

```bash
npm install
```

Levantar:

```bash
ng serve
```

o

```bash
npm start
```

Frontend:

```text
http://localhost:4200
```

---

# Herramientas utilizadas durante desarrollo

## API Testing

Thunder Client:

Usado para probar:

* login
* registro
* endpoints protegidos

Instalar desde extensiones VS Code.

---

## Base de datos

MySQL Workbench usado para:

* scripts SQL
* consultas
* gestión roles/usuarios

---

# Comandos usados en el proyecto

## Backend

Levantar:

```powershell
.\mvnw spring-boot:run
```

Compilar:

```powershell
.\mvnw clean install
```

---

## Git

Actualizar:

```bash
git pull origin main
```

Guardar cambios:

```bash
git add .
git commit -m "mensaje"
git push
```

---

# Problemas comunes

## Puerto 8080 ocupado

```powershell
netstat -ano | findstr :8080
```

---

## Dependencias Maven fallan

Ejecutar:

```powershell
.\mvnw clean
.\mvnw install
```

---

## node_modules faltante

```bash
npm install
```

---

## No conecta MySQL

Revisar:

* servicio iniciado
* password correcta
* puerto 3306
* application.properties

---

# Versiones recomendadas (usar estas)

```text
Java 21
Spring Boot 3.x
MySQL 8
Node LTS
VS Code latest
```

Evitar mezclar versiones distintas para no tener resultados diferentes.

---

# Checklist de entorno listo

## Backend

* [ ] Java instalado
* [ ] Maven wrapper funcionando
* [ ] MySQL levantado
* [ ] Base creada
* [ ] application.properties configurado
* [ ] Backend corre en 8080

---

## Frontend

* [ ] Node instalado
* [ ] npm install ejecutado
* [ ] Frontend corre en 4200

---

## Herramientas

* [ ] VS Code
* [ ] Thunder Client
* [ ] MySQL Workbench
* [ ] Git funcionando

---

# Flujo recomendado para nuevos integrantes

1.

Clonar repo

2.

Levantar MySQL

3.

Configurar application.properties

4.

Levantar backend

5.

Levantar frontend

6.

Probar que ambos servicios responden.

---

Entorno base replicado correctamente cuando:

✅ Backend corre en 8080

✅ Frontend corre en 4200

✅ MySQL conecta

Con eso deberían tener exactamente el mismo ambiente de desarrollo.

---

# Estado de Despliegue (Producción - Railway)

El backend está desplegado y operativo en Railway:

```text
https://ma-tpy1101-seccion001d-grupo6-production-cec7.up.railway.app
```

Base de datos MySQL 8.4 también en Railway. La app Android (rama `app-android`) consume directamente esta API en producción, sin necesidad de levantar el entorno local.

## Servicios externos integrados

* **Firebase Authentication** (Google Sign-In) — login social con selección de rol para usuarios nuevos
* **Webpay Plus (Transbank, sandbox)** — pasarela de pago real en ambiente de integración
* **Jitsi Meet** — videollamadas entre cliente y tarotista

## Pruebas

Ver `Plan_Pruebas_ConectaTarot_Final.docx`, `Anexo_Plan_Pruebas_Incremental.docx` e `Informe_Ejecucion_Plan_Pruebas.docx` en `docs/` para el detalle de casos de prueba ejecutados.
