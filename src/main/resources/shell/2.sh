#!/bin/sh
cd /home/container
./cloudflared tunnel create {tunnel name}
cat << EOF > ~/.cloudflared/config.yml
# add cname cloudTunnelId.cfargotunnel.com
tunnel: cloudTunnelId
credentials-file: /app/.cloudflared/cloudTunnelId.json

ingress:
  - hostname: glitch-v2.yourdomain.com
    service: http://127.0.0.1:2048
  - hostname: glitch.yourdomain.com
    service: http://127.0.0.1:7681
  - service: http_status:404
EOF

nohup ./cloudflared tunnel run cloudTunnelId > /dev/null 2>&1 &