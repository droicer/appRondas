Proyecto – Sistema de Gestión para la Empresa (Capstone Project)

Este proyecto corresponde al trabajo final del curso Capstone Project – Sistemas, desarrollado como sistema móvil para la gestión interna de procesos, de acuerdo con el documento entregado por el equipo.

Incluye módulos como:

Registro de rondas

Captura de evidencias (fotos Base64)

Control de incidentes

Reportes

Gestión de usuarios

Integración con servicios externos

📌 Tabla de Contenidos

Descripción del Proyecto

Requisitos

Instalación

Estructura del Proyecto

Configuración de google-services.json

Comandos Git Útiles

Autores

📘 Descripción del Proyecto

Este sistema móvil forma parte del desarrollo del trabajo final y está orientado a mejorar:

El registro y control de rondas

La trazabilidad de incidentes

La identificación del personal

La evidencia visual con fotografías

La comunicación interna

La seguridad operativa


⚙️ Requisitos

Según el proyecto móvil desarrollado:
Android Studio Koala / Ladybug / Flamingo
JDK 11+
Gradle 8+
Min SDK 24
Firebase configurado

🛠️ Instalación

Clonar el repositorio:
git clone https://github.com/droicer/appRondas/tree/v2


Entrar al proyecto:

cd nombre-del-proyecto


Abrir en Android Studio.

📁 Estructura del Proyecto
/app
   ├── src
   ├── build.gradle
   └── google-services.json   ⬅️ requerido para Firebase
/gradle
README.md
settings.gradle

📄 Configuración de google-services.json

El proyecto utiliza Firebase, y el documento del Capstone indica el uso de módulos que requieren autenticación, base de datos y almacenamiento; por ello se debe incluir el archivo:

google-services.json

📌 Ruta obligatoria:
/app/google-services.json

🔧 Instrucciones:

Descargar google-services.json desde Firebase Console.
Pegar el archivo dentro de la carpeta app.
Verificar que no esté dentro de subcarpetas como:
/app/src
/app/debug
Limpiar y reconstruir el proyecto.

Autores:

Cacho Rojas, Rodolfo Jesús
Chavez Marin, Kevin Anghelou
Escalante Horna, Anderson Joel
Espinoza Carrera, Cesar Orlando
Sanchez Intor, Keriban Smith
