# syntax=docker/dockerfile:1
# Build with npm externally configured, then npm run build on the host.
# The context includes only the resulting public assets and runtime files.
FROM nginx:1.28-alpine
RUN apk add --no-cache python3 && mkdir -p /etc/nginx/gems
COPY dist/app/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint-app.sh
RUN chmod +x /docker-entrypoint-app.sh
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
ENTRYPOINT ["/docker-entrypoint-app.sh"]
