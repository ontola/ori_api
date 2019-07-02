FROM openresty/openresty:alpine

COPY conneg.lua /usr/local/openresty/lualib/conneg.lua
COPY index.html /www/data/index.html
COPY nginx.conf /etc/nginx/conf.d/default.conf

VOLUME /www/data/id
VOLUME /www/data/api
