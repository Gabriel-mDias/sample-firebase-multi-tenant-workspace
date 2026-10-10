# syntax=docker/dockerfile:1
FROM node:24.16.0-bookworm-slim
ENV CI=true FIREBASE_CLI_DISABLE_USAGE=true
RUN npm install --global firebase-tools@15.32.1 --registry=https://registry.npmjs.org \
    && firebase setup:emulators:ui
WORKDIR /emulator
COPY firebase.json /emulator/firebase.json
COPY start.sh /emulator/start.sh
RUN chmod +x /emulator/start.sh && mkdir -p /data
EXPOSE 9099 4000
HEALTHCHECK --interval=5s --timeout=5s --start-period=30s --retries=12 \
    CMD node -e "Promise.all([fetch('http://127.0.0.1:9099/'),fetch('http://127.0.0.1:4000/')]).then(r=>process.exit(r.every(v=>v.ok)?0:1)).catch(()=>process.exit(1))"
ENTRYPOINT ["/emulator/start.sh"]
