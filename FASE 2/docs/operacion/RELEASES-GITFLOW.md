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
   └─ release/vN            (estabilización: solo correcciones y documentación)
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
git switch -c release/vN
# … solo correcciones de último momento, notas y versión de la documentación …
git push -u origin release/vN
# En GitHub: PR release/vN → main. Esperar el CI verde y hacer merge.

git switch main && git pull
git tag -a vN -m "vN: <resumen de una línea>"
git push origin vN                 # dispara build → test → imágenes → deploy

git switch develop && git merge --no-ff main && git push   # develop recibe lo de la release
git push origin --delete release/vN                         # opcional: limpiar la rama
```

Luego, en GitHub → Releases → *Draft a new release* sobre el tag `vN`, se
pegan las notas (plantilla al final) y se adjunta la evidencia:
- El enlace al run del pipeline.
- El resultado del smoke.
- Los reportes de pruebas (artefactos `pruebas-*`).

## Contenido propuesto de cada release

| Release | Contenido | Gate (lo verifica el pipeline o el responsable) |
|---|---|---|
| `v1` | Cierre de Fase 2 (documentación, ADR-011/013/014/015, prueba de fuga estable) y **frente DevOps**: pipeline CI/CD, Compose de producción, scripts de despliegue, smoke, respaldo y rollback, ADR-017 y runbook | 5 proyectos con sus pruebas en verde; 6 imágenes publicadas; **primer despliegue en GCP** y smoke externo en verde |
| `v2` | Dominio de crédito: modelo y migración, proyección del historial ERP y motor de elegibilidad versionado y explicable (Jorge), con **5 pruebas unitarias** | Pruebas unitarias del crédito en verde; despliegue sano |
| `v3` | Checkout con ruta crédito/PayKlik y Saga dual (Jairo); eventos, notificación, seguridad y auditoría del crédito, con **5 pruebas de integración y 5 E2E** (Edson) | Integración y E2E en verde; despliegue sano |
| `v4` | **5 pruebas de carga y 5 de estrés** contra la nube (Andy), Manual Técnico y de Usuario, DDA y diagramas finales | Las 25 pruebas documentadas; smoke externo; **rollback ensayado** |

Si un release no llega a su gate, no se etiqueta: se corrige en
`release/vN` o se mueve el contenido al siguiente.

## Ensayo de rollback (evidencia para v4)

1. Con `vN` desplegado, en GitHub → Actions → **CI/CD - SGO-Logic** → *Run
   workflow* → `deploy_tag` = `vN-1`.
2. Verificar que el smoke queda verde y que en la VM `cat .current_tag` dice `vN-1`.
3. Repetir con `deploy_tag` = `vN` para volver.
4. Guardar los enlaces de los dos runs como evidencia.

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
