#!/bin/sh

cat << EOF > ~/.cloudflared/config.yml
# add cname 499f6077-2b07-407c-aff9-8a6de881cb763.cfargotunnel.com
tunnel: 499f6077-2b07-407c-aff9-8a6de881cb763
credentials-file: /home/container/.cloudflared/499f6077-2b07-407c-aff9-8a6de881cb763.json

ingress:
  - hostname: wraithnodes-v2.yourdomain.com
    service: http://127.0.0.1:2048
  - hostname: wraithnodes.yourdomain.com
    service: http://127.0.0.1:7681
  - service: http_status:404
EOF

nohup ./cloudflared tunnel run 499f6077-2b07-407c-aff9-8a6de881cb763 > /dev/null 2>&1 &