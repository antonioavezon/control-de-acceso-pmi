# Control de acceso PMI

Aplicación **gratuita** para **Android** que permite gestionar invitados, enviar invitaciones con código QR personal y controlar el acceso a un evento usando el **PMI ID** como identificador único.

**Autor:** Antonio Avezon  
**Cargo:** Analista de experiencia, desarrollo y soluciones informáticas del voluntariado  
**Plataforma:** solo Android  
**Licencia de uso:** gratuita (código abierto en este repositorio)

Repositorio: [https://github.com/antonioavezon/control-de-acceso-pmi](https://github.com/antonioavezon/control-de-acceso-pmi)

---

## ¿Para qué sirve?

Flujo típico del evento:

1. Cargar lista de participantes  
2. (Opcional) Cargar plantilla de correo  
3. Enviar invitaciones con QR  
4. Controlar acceso escaneando el QR  
5. Consultar resumen de asistencia  
6. Descargar el resumen CSV  
7. Eliminar los datos del evento en la app (sin borrar el archivo ya descargado)

---

## Funcionalidades

### Carga de participantes
- Importa un archivo CSV / texto con el formato:
  - `NOMBRE | APELLIDOS | PMIID | EMAIL`
- Valida columnas, PMI ID (solo números), correos, duplicados y filas vacías.
- Muestra mensajes claros si el archivo está mal formado (sin cerrar la app).

### Plantilla de correo (`email.txt`)
- Primera línea = **asunto**
- Resto = **cuerpo**
- Puede usarse `****NOMBRE****` para personalizar el saludo.
- Si no se carga plantilla, la app usa un texto genérico.
- La app agrega automáticamente (en negrita) que el **QR va adjunto**.

### Envío de invitaciones
- Genera un QR único asociado al PMI ID de cada persona.
- Abre el cliente de correo del teléfono con asunto, cuerpo y QR adjunto.
- Permite enviar uno a uno o usar **Invitar a todos**.

### Control de acceso
- Escanea el QR en la entrada del evento.
- Resultado principal: **PERMITIDO** o **NO PERMITIDO**.
- Primer ingreso: registra hora de entrada (inmutable).
- Reingreso: permite el acceso sin duplicar asistentes ni cambiar la hora.

### Resumen del evento
- Total de invitados
- Asistentes únicos
- Personas que aún no ingresan
- Listado de asistentes registrados
- Exportación CSV:
  - `PMIID,NOMBRE,APELLIDOS,MAIL,HORAENTRADA`
  - línea final `TOTAL ASISTENTES: N`

### Seguridad de datos
- **Eliminar datos** solo se habilita después de haber exportado el resumen al menos una vez.
- Pide confirmación antes de borrar.
- Borra solo la base SQLite del evento; **no elimina** los CSV ya descargados.

---

## Instalación con el APK

1. Compila un APK release (ver más abajo) o usa el APK que generes localmente.
2. Copia el archivo `.apk` al teléfono (por ejemplo a **Descargas**).
3. En Android, permite instalar apps de fuentes desconocidas / del administrador de archivos.
4. Abre el APK e instálalo.
5. Abre **Control de acceso PMI**.

> Nota: este repositorio **no incluye** el APK firmado ni el keystore (por seguridad). Debes generarlos en tu entorno.

### Generar el APK (desarrollo)

Requisitos aproximados:
- JDK 17 o 21
- Android SDK

```bash
# En Fedora/RHEL, por ejemplo:
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew assembleRelease
```

El APK queda en:

`app/build/outputs/apk/release/app-release.apk`

Para firmar en release, copia `keystore.properties.example` a `keystore.properties` y configura tu keystore local (ese archivo **no** se sube a GitHub).

---

## Archivos necesarios para usar la app

Coloca estos archivos en el teléfono (recomendado: carpeta **Descargas**) y cárgalos desde la app.

### 1) Lista de participantes

Ejemplo incluido en el repo:

[`examples/invitados.example.csv`](examples/invitados.example.csv)

```text
NOMBRE | APELLIDOS | PMIID | EMAIL
Maria | Perez Gomez | 1234567 | maria.perez@ejemplo.com
Carlos | Ruiz Soto | 7654321 | carlos.ruiz@ejemplo.com
Ana | Lopez Diaz | 9988776 | ana.lopez@ejemplo.com
```

Reglas:
- Separador `|` o `,`
- PMI ID: solo números (sin puntos ni guion)
- Sin PMI ID duplicados
- Email con formato válido

### 2) Plantilla de correo (opcional)

Ejemplo incluido en el repo:

[`examples/email.example.txt`](examples/email.example.txt)

```text
Invitación – Encuentro de Voluntarios PMI | 5 de septiembre de 2026
Estimado/a ****NOMBRE****:

PMI le invita a participar en un Encuentro de Voluntarios...
```

- Línea 1 = asunto  
- Líneas siguientes = cuerpo  
- Se respetan saltos de línea del archivo  

---

## Privacidad

- La app trabaja **offline** (lista, QR, asistencia y exportación en el dispositivo).
- El envío de correo usa el **cliente de correo del teléfono**; no hay servidor propio de la app.
- Este repositorio **no incluye** archivos con datos personales reales (`email.txt`, `invitados.csv`, APK firmados ni keystores).
- Los ejemplos de este README usan datos ficticios.

---

## Compilar y probar

```bash
./gradlew test
./gradlew assembleDebug
```

---

## Créditos

Desarrollado por **Antonio Avezon**  
Analista de experiencia, desarrollo y soluciones informáticas del voluntariado  

Aplicación gratuita para la comunidad de voluntariado PMI.
