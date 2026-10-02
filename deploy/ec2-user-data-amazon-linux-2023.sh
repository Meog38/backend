#!/usr/bin/env bash
set -euxo pipefail

dnf update -y
dnf install -y docker openssl
systemctl enable --now docker
usermod -aG docker ec2-user

install -d -m 755 /opt/brainvest