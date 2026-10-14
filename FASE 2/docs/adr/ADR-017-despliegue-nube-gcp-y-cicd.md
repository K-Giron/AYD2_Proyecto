# ADR-017 — Despliegue en Google Cloud y pipeline CI/CD

- **Fecha:** 2026-10-13
- **Estado:** Accepted
- **Ratificación:** 2026-10-14
- **Responsables:** Kevin Girón (KEV3-01 a KEV3-08)
- **Revisores:** Andy Sánchez (validación cruzada del frente DevOps), Jairo Gómez

## Contexto

La Fase 3 pide desplegar en la nube la arquitectura documentada, con Docker
para backend y frontend y un pipeline `build → test → deploy` en GitHub
Actions o GitLab, sin desplegar si las pruebas fallan, y con al menos cuatro
releases bajo git-flow (tags `v1`, `v2`, …).

La arquitectura ejecutable de Fase 2 son 12 contenedores (ADR-009, ADR-015):
SGO-Logic (`frontend`, `backend`, `postgres`, `rabbitmq`) y cuatro sistemas
propios con su base (almacén central, tienda POS-01, PayKlik y LogiExpress).
El DDA fija un inicio on-premises con portabilidad a nube (EaC-11) y un costo
de infraestructura ≤ USD 1,200/mes (EaC-10).

## Drivers relacionados

- EaC-09 (mantenibilidad: desplegar con runbook en ≤ 2 h).
- EaC-10 (costo).
- EaC-11 (portabilidad: mismo artefacto contenedorizado en nube, sin reescribir el núcleo).
- Fase 3: CI/CD con gate de pruebas, Docker en backend y frontend, cuatro releases.

## Decisión

1. **Proveedor y topología:** una VM de **Google Compute Engine**
   `e2-standard-2` (2 vCPU, 8 GB) con Ubuntu 24.04, disco de 30 GB y Docker
   Compose. Es la misma topología de Fase 2: el artefacto no cambia, solo
   dónde corre.
2. **Imágenes inmutables en GitHub Container Registry (GHCR):** el pipeline
   construye las 6 imágenes de aplicación y las publica como
   `ghcr.io/<owner>/sgo-logic-<servicio>:<tag>`, con el tag de release (`v1`…)
   y `sha-<commit>`. Nunca se usa `latest` ni se compila en el servidor.
3. **Pipeline** (`.github/workflows/ci-cd.yml`):

   ```text
   test-java (5 proyectos, matriz) ─┐
                                    ├─► images (6 imágenes) ─► deploy (solo tags v*)
   test-frontend (JS + Compose) ────┘
   ```

   - `images` depende de **todas** las pruebas y `deploy` depende de
     `images`: **si una prueba falla, no se publica ni se despliega nada.**
   - En pull requests y ramas de git-flow se compila y prueba todo; las
     imágenes se construyen pero no se publican.
   - Al empujar un tag `vN` se repiten las pruebas, se publican las imágenes y
     se despliega en la VM.
   - Una ejecución manual (`workflow_dispatch`) despliega un tag ya publicado:
     es el **rollback manual**.
4. **Despliegue** (`deploy/scripts/deploy.sh`, ejecutado por SSH en la VM):
   1. Respaldo de las 5 bases.
   2. `pull` de la versión nueva y `up -d --no-build`.
   3. Espera a que los 12 contenedores estén `healthy`.
   4. **Smoke** (`deploy/scripts/smoke.sh`): salud de cada sistema, login
      JWT real, ruta autorizada (200) y protegida (401), catálogo y portales.
   5. Si algo falla, **rollback automático** al tag anterior. El job termina en
      error para que el fallo quede visible.

   El pipeline repite el smoke desde internet contra la IP pública.
5. **Configuración de producción** (`deploy/docker-compose.prod.yml`): se
   combina con el Compose base, sin duplicarlo.
   - Solo se publican la web (puerto 80) y los portales (8082, 8084, 8085, 8086).
   - Las bases, RabbitMQ y la API no quedan expuestas.
   - Cada JVM tiene un tope de memoria para caber en 8 GB.
   - Los contenedores usan `restart: always`.
6. **Secretos fuera del repositorio:** el `.env` de producción vive en el
   secreto `PROD_ENV_FILE` de GitHub, a partir de la plantilla
   `deploy/.env.production.example`. La llave SSH vive en `GCP_VM_SSH_KEY`. La
   IP y el usuario de la VM son variables del repositorio (`GCP_VM_HOST`,
   `GCP_VM_USER`). El acceso de la VM a GHCR usa el `GITHUB_TOKEN` efímero del
   job, y se cierra al terminar.

## Alternativas evaluadas

1. **VM + Compose en GCP (elegida):** reutiliza la topología probada, tiene
   el menor riesgo con la entrega a días y su costo es predecible. No escala
   horizontalmente de forma automática.
2. **Cloud Run por servicio:** escala solo, pero exige reemplazar PostgreSQL
   y RabbitMQ por servicios administrados (Cloud SQL ×5, Pub/Sub u otro
   broker) y rehacer redes y secretos. Es un cambio de arquitectura que no se
   puede validar a tiempo.
3. **GKE (Kubernetes):** es la ruta natural si el sistema crece, pero agrega
   manifiestos, ingreso y operación que el equipo no domina. Queda como
   evolución.
4. **Azure for Students o AWS:** técnicamente equivalentes. Se eligió GCP por
   el crédito disponible del equipo para una VM de 8 GB; la capa gratuita de
   AWS (1 GB) no alcanza.

## Consecuencias

- **Costo aproximado:** `e2-standard-2` en `us-central1` ≈ USD 50/mes, más
  disco e IP. Cumple EaC-10 con amplio margen y lo cubre el crédito de prueba
  de GCP.
- **Portabilidad (EaC-11):** las mismas imágenes corren en local, en CI y en
  la VM. Pasar a otro proveedor solo cambia los valores de la VM, no el
  pipeline ni el código.
- **Las migraciones Flyway no se revierten:** un rollback a una versión
  anterior funciona mientras las migraciones nuevas sean aditivas (regla ya
  usada en Fases 2 y 3). Ante una migración destructiva, la recuperación es
  restaurar el respaldo previo (`backups/`) siguiendo el runbook.
- Una sola VM es un punto único de falla. Es aceptable para el MVP académico
  y queda documentado como riesgo.

## Riesgos

- **Portales sin contraseña:** los portales de la tienda, PayKlik y
  LogiExpress no piden contraseña (decisión de alcance de ADR-015) y quedan
  públicos en la VM. Mitigación: la regla de firewall de esos puertos se puede
  limitar a las IP del equipo y de los calificadores.
- **Memoria:** 6 JVM en 8 GB. Mitigación: topes `-Xmx`, swap de 4 GB
  (`vm-bootstrap.sh`) y monitoreo durante las pruebas de estrés (Andy).
- **Permisos del repositorio:** crear secretos y variables exige ser
  administrador del repositorio (dueño: Jairo Gómez).

## Criterios de validación o ratificación

- [x] El pipeline pasa `actionlint` y `shellcheck`.
- [x] `deploy.sh` probado con imágenes reales en un registro local: una
  versión sana se despliega; una versión que no queda sana se **revierte sola**
  a la anterior y el job termina en error (2026-10-13).
- [x] `smoke.sh` en verde contra el sistema completo (10 de 10
  verificaciones, 2026-10-13).
- [x] Infraestructura creada (2026-10-14): proyecto `sgo-logic-g5-202010844`,
  VM `sgo-logic-vm` (`us-central1-a`, `e2-standard-2`, Ubuntu 24.04), IP fija
  `136.65.180.39` y firewall `sgo-logic-web`.
- [x] Primer despliegue (`v0-manual`, imágenes construidas en la VM porque GHCR
  aún no tenía imágenes): 12 de 12 contenedores sanos y smoke 10 de 10 dentro
  de la VM y **desde internet**. Uso de memoria: 2.6 de 7.7 GB.
- [x] Primer despliegue `v1` por el pipeline:
  [run 38554309937](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38554309937),
  con pruebas, imágenes, deploy y smoke en verde.
- [ ] Un PR con una prueba rota muestra el job `deploy` sin ejecutarse (evidencia del gate).
- [ ] Un rollback manual (`workflow_dispatch` con el tag anterior) queda registrado.
- [x] Revisión estática cruzada de Andy Sánchez sobre workflow, Compose,
  respaldo, health, smoke y rollback; observaciones conservadas como gates
  operativos.

## Ratificación

La decisión de usar GCE, Docker Compose, GHCR y GitHub Actions se acepta porque
la topología documentada ya opera sin reescribir el núcleo. Además del release
`v1`, el SHA `274ee02` completó el
[run de CI 38616631224](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38616631224)
y el
[run de despliegue 38618417187](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38618417187),
incluidos health checks y smoke desde internet.

La aceptación del ADR ratifica la decisión arquitectónica, no declara cerrado
todo el frente. Continúan abiertos como evidencia operativa: ejecutar un gate
negativo controlado, registrar un rollback manual a un tag anterior, comprobar
una restauración de respaldo y publicar `v2`, `v3` y `v4`.

## Referencias

- `.github/workflows/ci-cd.yml`, `FASE 2/deploy/` (Compose de producción,
  plantilla de variables y scripts).
- `FASE 2/docs/operacion/GUIA-GCP-Y-PIPELINE.md` (puesta en marcha paso a paso).
- `FASE 2/docs/operacion/RUNBOOK.md` (operación, rollback y respaldo).
- `FASE 2/docs/operacion/RELEASES-GITFLOW.md` (releases `v1`–`v4`).
- ADR-009 (topología ejecutable), ADR-015 (sistemas propios), DDA EaC-09/10/11.
