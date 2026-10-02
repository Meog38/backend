#!/usr/bin/env bash
set -euxo pipefail

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io openssl ca-certificates curl

systemctl enable --now docker
usermod -aG docker ubuntu || true

install -d -m 755 /opt/brainvest
install -d -m 700 /var/lib/brainvest
