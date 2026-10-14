# Revisión cruzada CI/CD — Fase 3

**Revisor:** Andy Sánchez  
**Responsable del frente revisado:** Kevin Girón  
**Fecha:** 2026-10-14  
**Base revisada:** `develop` en `e551994`; release `v1` en `de7cf97`  
**Alcance:** revisión estática de workflow, Compose de producción, deploy,
health, smoke, respaldo y rollback. Esta evidencia no sustituye los enlaces de
ejecuciones reales de GitHub Actions.

## Archivos revisados

- `.github/workflows/ci-cd.yml`
- `deploy/docker-compose.prod.yml`
- `deploy/scripts/deploy.sh`
- `deploy/scripts/smoke.sh`
- `deploy/scripts/backup.sh`
- `docs/operacion/GUIA-GCP-Y-PIPELINE.md`
- `docs/operacion/RUNBOOK.md`
- `docs/operacion/RELEASES-GITFLOW.md`

## Resultado

| Criterio de AND3-07 | Evidencia en código | Resultado |
|---|---|---|
| Build y pruebas preceden al despliegue | `images` depende de `test-java` y `test-frontend`; `deploy` depende de `images` | CUMPLE |
| Una prueba fallida impide desplegar un tag nuevo | Para tags, `deploy` exige `needs.images.result == 'success'` | CUMPLE |
| Se prueban los cinco proyectos Java | Matriz: backend, inventario, tienda, PayKlik y LogiExpress; todos ejecutan `clean verify` | CUMPLE |
| Frontend y servicios se construyen con Docker | Matriz de seis imágenes, incluida frontend | CUMPLE |
| Las imágenes son trazables | Tags `sha-<7>` y `vN`; etiquetas OCI con repositorio y SHA | CUMPLE |
| Secretos fuera de YAML e imágenes | SSH y archivo de producción provienen de Secrets; Compose recibe variables | CUMPLE EN DISEÑO |
| Health y smoke posteriores al deploy | `deploy.sh` espera 12 servicios sanos y ejecuta smoke; el workflow repite smoke externo | CUMPLE |
| Despliegue fallido revierte | `deploy.sh` conserva `.current_tag`, intenta el tag previo y vuelve a comprobar salud/smoke | IMPLEMENTADO; ENSAYO PENDIENTE |
| Respaldo anterior al despliegue | `backup.sh` genera dumps comprimidos de cinco bases y conserva siete copias | IMPLEMENTADO; RESTAURACIÓN PENDIENTE |
| Cuatro releases trazables | Existe `v1`; el documento define `v2`, `v3` y `v4` | PENDIENTE |

## Observaciones

1. El workflow acepta cualquier tag cuyo nombre comience con `v`; no comprueba
   técnicamente que el commit etiquetado pertenezca a `main`. La guía git-flow
   sí lo exige. Antes de cada release debe verificarse esa pertenencia o agregar
   un gate automático.
2. Un fallo del respaldo previo se registra como advertencia y el despliegue
   continúa. Para la entrega académica es obligatorio conservar evidencia de
   un respaldo válido antes del ensayo de rollback.
3. El camino manual (`workflow_dispatch`) omite compilación deliberadamente y
   solo debe recibir un tag inmutable ya publicado. Debe restringirse mediante
   el ambiente protegido `produccion` y aprobación humana.
4. La revisión estática no demuestra que Secrets, permisos de GHCR, firewall o
   acceso SSH estén configurados. Eso se prueba únicamente con el run del tag.

## Evidencia que falta anexar

- URL del run verde que creó y desplegó `v1`.
- Artefactos `pruebas-*` de los cinco proyectos.
- Salida del smoke externo del tag medido.
- Run de rollback a `vN-1` y run de retorno a `vN`.
- Confirmación de un respaldo y restauración controlada.
- Tags y notas de `v2`, `v3` y `v4`.

## Veredicto

**APROBACIÓN CONDICIONADA.** El diseño del pipeline cumple el orden
build/test/image/deploy y bloquea el despliegue de un tag nuevo si falla un
gate. El frente no puede marcarse final hasta anexar los runs reales, ensayar
rollback/restauración y completar los cuatro releases.
