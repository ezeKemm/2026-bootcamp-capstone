# Frontend image, built from source in one multi-stage build
# Build context: frontend/
# `docker build -f frontend/Dockerfile --build-arg GIT_SHA=<commit> -t northstar-crm-frontend:local frontend`
# The unprivileged nginx image listens on 8080 and runs as a non-root user.
# TODO: pin both base images by digest (Dependabot's docker ecosystem then keeps them current).
ARG NODE_IMAGE=node:22-alpine
ARG WEB_IMAGE=nginxinc/nginx-unprivileged:stable-alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration=production \
 && mkdir /site && cp -r dist/*/browser/. /site/

FROM ${WEB_IMAGE}
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="northstar-crm-frontend" \
      org.opencontainers.image.revision="${GIT_SHA}"
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /site/ /usr/share/nginx/html/
EXPOSE 8080
# Serves the Angular build.
