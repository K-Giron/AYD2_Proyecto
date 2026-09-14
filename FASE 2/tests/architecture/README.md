# Controles de arquitectura

Los controles ejecutables viven en `backend/src/test/java/com/megastore/sgologic/ModularArchitectureTest.java` y validan:

- estructura reconocida por Spring Modulith;
- conjunto exacto de doce módulos directos;
- ausencia actual de dependencias entre módulos no aprobadas.

Cuando el equipo apruebe una dependencia pública, debe actualizar simultáneamente el contrato, la declaración modular y esta prueba. No se relaja el control para aceptar accesos a repositorios, entidades, adaptadores o tablas ajenas.
