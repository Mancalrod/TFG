# =======================================
# Stage 1: Build
# =======================================
FROM node:20-alpine AS build

WORKDIR /app

# Copiar package.json y lock primero (cache de capas)
COPY package.json package-lock.json ./
RUN npm ci

# Copiar código fuente y compilar
COPY . .
RUN npm run build

# =======================================
# Stage 2: Runtime (Nginx)
# =======================================
FROM nginx:alpine

# Copiar build de React
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
