# Familie-prosessering

Prosesseringsmotor for familieområdet.

## Generellt
* Oppdater status i task-tabellen til lengde 20: `ALTER TABLE task ALTER COLUMN status VARCHAR(20)  DEFAULT 'UBEHANDLET'::CHARACTER VARYING NOT NULL;`


![](https://github.com/navikt/familie-prosessering-backend/workflows/Build-Deploy/badge.svg)
