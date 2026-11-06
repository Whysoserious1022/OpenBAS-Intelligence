# ============================================================
# OpenBAS Intelligence — Frontend Dockerfile
# React + TypeScript + Vite industrial operations dashboard
# ============================================================

# --- Stage 1: Build ---
FROM node:20-alpine AS build

WORKDIR /app

# Copy package manifests and install dependencies (layer caching)
COPY package*.json ./
RUN npm ci --prefer-offline

# Copy source and build
COPY . .
RUN npm run build

# --- Stage 2: Serve ---
FROM nginx:1.27-alpine AS runtime

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Custom nginx config: SPA routing (all routes → index.html) + gzip
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Non-root user (nginx already runs as nginx)
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]

