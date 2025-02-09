#!/bin/sh
cd /home/container
wget https://github.com/XTLS/Xray-core/releases/download/v25.1.30/Xray-linux-64.zip
unzip Xray-linux-64.zip
rm -rf Xray-linux-64.zip
chmod 755 xray
cat << EOF > ali.json
{
  "outbounds": [
    {
      "protocol": "freedom",
      "tag": "direct"
    },
    {
      "protocol": "blackhole",
      "tag": "blocked"
    }
  ],
  "log": {
    "loglevel": "warning"
  },
  "routing": {
    "rules": [
      {
        "type": "field",
        "outboundTag": "direct",
        "ip": [
          "10.0.0.0\/8",
          "172.16.0.0\/12",
          "192.168.0.0\/16"
        ]
      }
    ],
    "domainStrategy": "IPOnDemand"
  },
  "inbounds": [
    {
      "port": 2048,
      "protocol": "vmess",
      "streamSettings": {
        "network": "ws",
        "wsSettings": {
        },
        "security": "none"
      },
      "settings": {
        "clients": [
          {
            "id": "d65bed55-98b2-41b9-9109-2b655d6385db",
            "level": 1,
            "alterId": 16
          }
        ],
        "decryption": "none"
      }
    }
  ]
}
EOF

nohup ./xray run --config ali.json > /dev/null  2>&1 &



wget https://github.com/tsl0922/ttyd/releases/download/1.6.3/ttyd.x86_64
mv ttyd.x86_64 ttyd
chmod 755 ttyd
nohup ./ttyd bash > /dev/null  2>&1 &


wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
mv cloudflared-linux-amd64 cloudflared
chmod 755 cloudflared
./cloudflared tunnel login

./cloudflared tunnel create wraithnodes