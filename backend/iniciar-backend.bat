@echo off
rem Inicia el backend de Reservas con doble clic (API en http://localhost:8081).
rem Cerrar esta ventana detiene el servidor.
title Backend Reservas - http://localhost:8081
cd /d "%~dp0"
echo Iniciando backend en http://localhost:8081 ...
echo Consola H2: http://localhost:8081/h2-console  (jdbc:h2:mem:reservas, usuario sa)
echo Para detenerlo, cierra esta ventana o presiona Ctrl+C.
echo.
call "%~dp0mvnw.cmd" spring-boot:run
echo.
echo El backend se detuvo.
pause
