#!/usr/bin/env python3
"""Genera NUEVO-modelo-datos-fase2.dot a partir de las migraciones Flyway reales
(CREATE TABLE / ALTER TABLE ... ADD COLUMN / REFERENCES). Uso:
    python3 docs/diagrams/generar-er-fase2.py && sh docs/diagrams/render.sh
"""
import re, pathlib, html

ROOT = pathlib.Path(__file__).resolve().parents[2]
SYSTEMS = [
    # (título, color, carpeta de migraciones, versiones a incluir, archivo de salida)
    ("Almacén central (inventory-service, inventory-db)", "#D1FAE5", ROOT / "inventory-service/src/main/resources/db/migration", None, "central"),
    ("Tienda física POS-01 (store-service, store-db)", "#FFEDD5", ROOT / "store-service/src/main/resources/db/migration", None, "tienda-pos01"),
    ("PayKlik (payklik-service, payklik-db)", "#EDE9FE", ROOT / "payklik-service/src/main/resources/db/migration", None, "payklik"),
    ("LogiExpress (logiexpress-service, logiexpress-db)", "#FEF08A", ROOT / "logiexpress-service/src/main/resources/db/migration", None, "logiexpress"),
    ("SGO-Logic — tablas NUEVAS de Fase 2 (onlinestore, inventorysync, customers, integration)", "#DBEAFE",
     ROOT / "backend/src/main/resources/db/migration",
     {"V1007", "V1008", "V1009", "V1010", "V1012", "V1013", "V2004", "V1011", "V4003"}, "sgo-logic-nuevas"),
]
CREATE = re.compile(r"CREATE TABLE\s+(\w+)\s*\((.*?)\n\);", re.S | re.I)
ALTER_ADD = re.compile(r"ALTER TABLE\s+(\w+)\s+ADD COLUMN\s+(?:IF NOT EXISTS\s+)?(\w+)\s+([\w()]+(?:\s*\(\s*\d+\s*(?:,\s*\d+)?\))?)", re.I)

def parse(folder, only):
    tables, fks, altered = {}, [], {}
    for f in sorted(folder.glob("V*.sql"), key=lambda p: int(re.match(r"V(\d+)", p.name).group(1))):
        if only and f.name.split("__")[0] not in only:
            continue
        sql = re.sub(r"--[^\n]*", "", f.read_text())
        for name, body in CREATE.findall(sql):
            cols = []
            for line in re.split(r",\s*\n", body):
                line = " ".join(line.split())
                if not line or re.match(r"(CONSTRAINT|PRIMARY KEY|UNIQUE|CHECK|FOREIGN KEY)", line, re.I):
                    m = re.search(r"FOREIGN KEY\s*\((\w+)\)\s*REFERENCES\s+(\w+)", line, re.I)
                    if m: fks.append((name, m.group(1), m.group(2)))
                    continue
                parts = line.split(" ")
                col, typ = parts[0], parts[1] if len(parts) > 1 else ""
                typ = re.sub(r"\(.*", "", typ)
                pk = "PRIMARY KEY" in line.upper()
                m = re.search(r"REFERENCES\s+(\w+)", line, re.I)
                if m: fks.append((name, col, m.group(1)))
                cols.append((col, typ, pk, bool(m)))
            tables[name] = cols
        for name, col, typ in ALTER_ADD.findall(sql):
            altered.setdefault(name, []).append((col, re.sub(r"\(.*", "", typ)))
    return tables, fks, altered

def header(title):
    return ['// NUEVO (Fase 2, Kevin Girón): generado por generar-er-fase2.py desde las migraciones reales.',
            '// No sustituye a SGO-Logic-Modelo-ER.drawio ni a modelo-datos.md.',
            'digraph ER_Fase2 {',
            f'  graph [label=<<B>NUEVO · Modelo de datos Fase 2 — {html.escape(title)}</B><BR/><FONT POINT-SIZE="10">Generado desde Flyway · cada sistema tiene su base y no lee tablas de otro (solo API o eventos)</FONT>>, labelloc=t, rankdir=LR, fontname="Arial", fontsize=14, bgcolor="#F8FAFC", nodesep=0.2, ranksep=0.6];',
            '  node [shape=plaintext, fontname="Arial", fontsize=8];',
            '  edge [color="#64748B", arrowsize=0.6];']

for old in (pathlib.Path(__file__).parent / "nuevos-fase2").glob("NUEVO-modelo-datos-fase2*"):
    old.unlink()
for i, (title, color, folder, only, slug) in enumerate(SYSTEMS):
    out = header(title)
    tables, fks, altered = parse(folder, only)
    for name, cols in altered.items():
        if name in tables:
            tables[name] = tables[name] + [(c, t, False, False) for c, t in cols]
    for name, cols in tables.items():
        rows = "".join(
            f'<TR><TD ALIGN="LEFT">{"<B>" if pk else ""}{html.escape(c)}{"</B>" if pk else ""}{" (PK)" if pk else ""}{" (FK)" if fk else ""}</TD><TD ALIGN="LEFT"><FONT COLOR="#64748B">{html.escape(t.lower())}</FONT></TD></TR>'
            for c, t, pk, fk in cols)
        out.append(f'  {name} [label=<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="3"><TR><TD COLSPAN="2" BGCOLOR="{color}"><B>{name}</B></TD></TR>{rows}</TABLE>>];')
    for name, cols in altered.items():
        if name in tables:
            continue
        rows = "".join(f'<TR><TD ALIGN="LEFT">+ {html.escape(c)}</TD><TD ALIGN="LEFT"><FONT COLOR="#64748B">{html.escape(t.lower())}</FONT></TD></TR>' for c, t in cols)
        out.append(f'  {name} [label=<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="3"><TR><TD COLSPAN="2" BGCOLOR="#FDE68A"><B>{name}</B> (columnas nuevas en Fase 2)</TD></TR>{rows}</TABLE>>];')
    for table, col, ref in fks:
        if ref in tables:
            out.append(f'  {table} -> {ref} [label="{col}", fontsize=7, fontname="Arial"];')
    out.append('}')
    target = pathlib.Path(__file__).parent / f"nuevos-fase2/NUEVO-modelo-datos-{slug}.dot"
    target.write_text("\n".join(out) + "\n")
    print("escrito", target.name, len(tables) + len([n for n in altered if n not in tables]), "tablas")
