FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_ZBLOG_API_BASE_URL=/api/v1
ENV VITE_ZBLOG_API_BASE_URL=${VITE_ZBLOG_API_BASE_URL}

RUN npm run build

FROM nginx:alpine

RUN apk add --no-cache curl

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 5173

CMD ["nginx", "-g", "daemon off;"]
