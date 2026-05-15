#!/bin/sh
set -eu

mkdir -p /app/storage
chown -R spring:spring /app/storage

exec gosu spring:spring java -jar app.jar
