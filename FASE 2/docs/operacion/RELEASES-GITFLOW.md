# Releases y git-flow — Fase 3

El enunciado exige git-flow, al menos **cuatro releases** con tags `v1`, `v2`,
`v3` y `v4`, commits con el formato `carné: mensaje` y que el merge de un PR
no cuente como commit. Este documento fija cómo se hace cada release y qué
lleva.

> El tag `v2.0.0` que ya existe pertenece a la entrega de Fase 2 y se
> conserva como historia. Los releases de Fase 3 usan el formato del
> enunciado: `v1`, `v2`, `v3`, `v4`.

## Flujo

```text
feature/<autor>-<tema>  (desde develop)
   └─ PR → develop          (CI: pruebas de los 5 proyectos + frontend)
develop
   └─ release/fase3-vN      (estabilización: solo correcciones y documentación)
        └─ PR → main        (CI de nuevo)
             └─ tag vN      → CI/CD publica imágenes y despliega en GCP
        └─ merge de vuelta a develop
hotfix/<tema>  (desde main) → main (tag vN.1) y develop
```

- Una feature se borra al integrarse en `develop`.
- `main` solo recibe `release/*` o `hotfix/*`.
- El despliegue lo dispara **el tag**. Si las pruebas del tag fallan, el
  pipeline no publica ni despliega (ADR-017).

## Comandos de cada release

Los ejecuta el responsable de releases (Kevin Girón). `N` es el número de release.

```bash
git switch develop && git pull
git switch -c release/fase3-vN
# … solo correcciones de último momento, notas y versión de la documentación …
git push -u origin release/fase3-vN
# En GitHub: PR release/fase3-vN → main. Esperar el CI verde y hacer merge (merge commit).

git switch main && git pull
git tag -a vN -m "vN: <resumen de una línea>"
git push origin vN                 # dispara build → test → imágenes → deploy

git switch develop && git merge --no-ff main && git push   # develop recibe lo de la release
```

Luego, en GitHub → Releases → *Draft a new release* sobre el tag `vN`, se
pegan las notas (plantilla al final) y se adjunta la evidencia:
- El enlace al run del pipeline.
- El resultado del smoke.
- Los reportes de pruebas (artefactos `pruebas-*`).

## Releases publicados

El crédito, la Saga dual y las pruebas se integraron en `develop` casi al mismo
tiempo, así que el contenido se agrupó distinto al plan original (que asignaba
crédito a `v2`, Saga y E2E a `v3`, y carga/estrés a `v4`). Cada release pasó su
gate: pruebas de los 5 proyectos, imágenes publicadas, despliegue sano y smoke.

| Release | Rama | Contenido | Gate verificado |
|---|---|---|---|
| `v1` | `release/v1` | Cierre de Fase 2 (ADR-011/013/014/015, prueba de fuga estable), avance del crédito (V4004–V4009) y **frente DevOps**: pipeline CI/CD, Compose de producción, scripts de despliegue, smoke, respaldo y rollback, ADR-017 y runbook | 5 proyectos y frontend en verde; 6 imágenes en GHCR; **primer despliegue automático en GCP**; smoke 10/10 |
| `v2` | `release/fase3-v2` | Crédito propio completo (línea individual, elegibilidad explicable, historial ERP, ciclo de la obligación), registro de clientes con BCrypt (V4010–V4011), Saga dual con degradación a PayKlik, seguridad 401/403, **pruebas unitarias, de integración y E2E**, **5 de carga y 5 de estrés** (k6), manuales y DDA | 323 pruebas del backend en verde; despliegue sano; smoke 17/17 |
| `v3` | `release/fase3-v3` | Cierre de documentación y operación: matriz de evidencia con los releases, flujo de releases, bitácora y nombre actual del repositorio | Pipeline en verde; despliegue sano; smoke 17/17 |
| `v4` | `release/fase3-v4` | Entrega final con el **ensayo de rollback** documentado | Rollback `v3 → v2 → v3` verificado; despliegue de `v4` sano |

Si un release no llega a su gate, no se etiqueta: se corrige en
`release/fase3-vN` o se mueve el contenido al siguiente.

Los despliegues manuales de `develop` (*Run workflow* con `deploy_tag =
develop`) se usaron para validar la integración en la nube antes de `v2`. Desde
`v2`, la nube solo recibe tags de release.

## Ensayo de rollback (evidencia para v4)

Se ensaya entre `v3` y `v2`, que no tienen migraciones de por medio:

1. Con `v3` desplegado, en GitHub → Actions → **CI/CD - SGO-Logic** → *Run
   workflow* → `deploy_tag` = `v2`.
2. Verificar que el smoke queda verde y que en la VM `cat .current_tag` dice `v2`.
3. Repetir con `deploy_tag` = `v3` para volver.
4. Guardar los enlaces de los dos runs como evidencia (sección siguiente).

### Resultado del ensayo

| Paso | Run | Versión en la nube | Smoke |
|---|---|---|---|
| Rollback `v3 → v2` | _pendiente_ | _pendiente_ | _pendiente_ |
| Regreso `v2 → v3` | _pendiente_ | _pendiente_ | _pendiente_ |

## Plantilla de notas de release

```markdown
## vN — <título>

**Fecha:** AAAA-MM-DD · **Commit:** <sha> · **Despliegue:** http://<IP>/ (run #<n>)

### Qué incluye
- …

### Pruebas
- Unitarias / integración / E2E: <resultado del pipeline, enlace a artefactos>
- Smoke en la nube: verde (10/10)

### Notas de operación
- Migraciones nuevas: …   (aditivas: sí/no)
- Rollback: desplegar `vN-1` con *Run workflow*.

### Contribuciones
| Integrante | Commits / PR |
|---|---|
```
