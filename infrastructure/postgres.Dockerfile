FROM postgres:17-alpine
COPY infrastructure/init-databases.sh /docker-entrypoint-initdb.d/01-databases.sh
RUN chmod 644 /docker-entrypoint-initdb.d/01-databases.sh
