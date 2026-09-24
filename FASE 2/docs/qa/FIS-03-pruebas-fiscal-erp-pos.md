# FIS-03 — Evidencia de pruebas fiscal e integraciones legacy

| Campo | Valor |
|---|---|
| Responsable | Andy Sánchez |
| Corte | 2026-09-22 |
| Estado | Implementado localmente; revisión cruzada pendiente |

## Cobertura automatizada agregada

| Área | Escenarios |
|---|---|
| Fiscal/SAT | XML escapado, CUI/serie, aprobación, rechazo, compuerta de confirmación, idempotencia y timeout con 3 reintentos |
| ERP | lote CSV, ventana vacía sin archivo, consistencia de totales, idempotencia por orden y timeout hasta `DEAD_LETTER` |
| POS | acuse para tienda 32, deduplicación evento+tienda y timeout hasta `DEAD_LETTER` |
| PayKlik | aprobación, rechazo y prohibición de PAN/CVV |
| Arquitectura | límites y ciclos mediante Spring Modulith |

## Resultado reproducible

Se ejecutó localmente la selección:

```bash
./mvnw -Djava.version=21 \
  -Dtest=FiscalServiceTest,ErpIntegrationServiceTest,PosIntegrationServiceTest,SimulatedPayKlikAdapterTest,ModularArchitectureTest \
  test
```

La selección contiene 19 pruebas después de agregar el caso positivo de nota de crédito. Todas están incluidas en la ejecución final descrita a continuación.

Después se ejecutó toda la suite que no usa contenedores:

```bash
./mvnw -Djava.version=21 -Dtest="!InfrastructureIntegrationTest" test
```

Resultado: `BUILD SUCCESS`; 40 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas. La aplicación también compiló con `./mvnw -Djava.version=21 -DskipTests compile`: `BUILD SUCCESS`.

## Límites de la evidencia

- La versión oficial del proyecto es Java 25; la máquina disponible tiene Java 21. Se usó la propiedad de Maven solo como verificación local de compatibilidad de fuentes, sin cambiar `pom.xml` ni el Dockerfile.
- Docker no está instalado en la máquina disponible. No se ejecutaron Testcontainers, migraciones contra PostgreSQL real, `clean verify`, Compose ni el smoke test.
- La validación con Java 25 y Docker debe hacerse antes de integrar a `develop`.
- BB-04/Outbox todavía no está disponible; fiscal, ERP y POS conservan estado e idempotencia propios, pero la entrega durable ante reinicio debe integrarse con el mecanismo transversal de Edson.
