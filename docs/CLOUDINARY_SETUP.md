# Configuración de Cloudinary

Cloudinary se usa en producción (Render) para almacenar archivos de forma **persistente**, evitando la pérdida de datos al reiniciar el despliegue.

## 1. Crear cuenta en Cloudinary (gratis)

1. Ir a [cloudinary.com/users/register_free](https://cloudinary.com/users/register_free)
2. Registrarse con email o Google
3. Verificar el email

## 2. Obtener credenciales

1. Acceder al **Dashboard** de Cloudinary
2. En la sección **"Product Environment Credentials"** encontrarás:
   - **Cloud Name** — nombre único de tu cuenta (ej: `dxyz1234`)
   - **API Key** — clave pública (ej: `123456789012345`)
   - **API Secret** — clave secreta (ej: `abcdefghijklmnopqrstuvw`)

> ⚠️ **Importante:** El API Secret es sensible. No lo subas a Git ni lo compartas públicamente.

## 3. Configurar en Render

En tu servicio de Render:

1. Ir a **Dashboard → Tu servicio → Settings → Environment**
2. Añadir estas variables de entorno:

| Variable | Valor | Ejemplo |
|---|---|---|
| `CLOUDINARY_ENABLED` | `true` | `true` |
| `CLOUDINARY_CLOUD_NAME` | Tu Cloud Name | `dxyz1234` |
| `CLOUDINARY_API_KEY` | Tu API Key | `123456789012345` |
| `CLOUDINARY_API_SECRET` | Tu API Secret | `abcdefghijklmnopqr...` |

3. Guardar y hacer **redeploy**

## 4. Verificar

Tras el redeploy:
1. Subir un archivo desde la aplicación
2. Verificar en el Dashboard de Cloudinary que aparece en la carpeta `tfg-entregables/`
3. Reiniciar el servicio en Render y comprobar que el archivo sigue accesible

## Desarrollo local

En desarrollo **no se necesita Cloudinary**. Los archivos se guardan en el directorio local configurado en `app.upload.dir` (`${user.home}/tfg-uploads` por defecto).

Si quieres probar Cloudinary en local, puedes usar las mismas credenciales con variables de entorno:

```bash
set CLOUDINARY_ENABLED=true
set CLOUDINARY_CLOUD_NAME=tu_cloud_name
set CLOUDINARY_API_KEY=tu_api_key
set CLOUDINARY_API_SECRET=tu_api_secret
.\mvnw.cmd spring-boot:run
```
