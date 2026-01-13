# Projeto de TDD E-Commerce — CI/CD com GitHub Actions, Docker (GHCR) e Kubernetes (k3s)

Este repositório contém uma aplicação **Java + Spring Boot (Maven)** com testes e um pipeline completo de **CI/CD** que:
- compila e executa testes automaticamente;
- gera e publica imagem Docker no **GitHub Container Registry (GHCR)**;
- realiza deploy automatizado em **Kubernetes (k3s)**;
- expõe a aplicação via **Ingress** com **DNS gratuito (nip.io)** e **HTTPS (Let's Encrypt)**.

---

## Sumário

- [Visão geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Executar localmente](#executar-localmente)
- [Testes](#testes)
- [Docker](#docker)
- [Kubernetes](#kubernetes)
- [Ingress, DNS e HTTPS](#ingress-dns-e-https)
- [CI/CD (GitHub Actions)](#cicd-github-actions)

---

## Visão geral

A aplicação disponibiliza um endpoint para finalização de compra e um endpoint de saúde (`/health`) utilizado por:
- probes do Kubernetes (liveness/readiness);
- verificação rápida do serviço após deploy.

O pipeline foi desenhado para funcionar em um ambiente onde já existe Nginx atendendo outras aplicações no meu Virtual Private Server (VPS), assim, foi feito um proxy para manter o isolamento e evitar conflito de portas com o cluster Kubernetes.

---

## Tecnologias

- **Java 17**
- **Spring Boot 3.x**
- **Maven**
- **Docker**
- **GitHub Actions**
- **GitHub Container Registry (GHCR)**
- **Kubernetes (k3s)**
- **Ingress**
- **Certbot / Let’s Encrypt** (TLS/HTTPS)

---

## Arquitetura

Fluxo de CI/CD:

1. **Push** na branch `main`
2. **GitHub Actions (CI)**:
   - `mvn clean verify` (build + testes)
   - build da imagem Docker
   - push da imagem para GHCR (`latest` e `SHA do commit`)
3. **GitHub Actions (CD)**:
   - aplica manifests Kubernetes (`k8s/app/*.yaml`)
   - atualiza a imagem do Deployment para a tag do commit
4. **Exposição HTTP/HTTPS**:
   - **ingress-nginx** expõe o Ingress via **NodePort** (`30080`/`30443`)
   - **Nginx** na VPS recebe `80/443` e faz proxy para o NodePort do Ingress
   - TLS com **Let’s Encrypt** no Nginx usando SNI por hostname

---

## Executar localmente

### Pré-requisitos
- Java 17
- Maven 3.9+

### Rodar a aplicação
```bash
mvn spring-boot:run
```

Teste:
```bash
curl http://localhost:8080/health
```

---
## Testes

### Executar testes (unitários + integração)
```bash
mvn clean verify
```

- Testes unitários rodam no ciclo padrão do Maven.
- Teste de integração básico via `@SpringBootTest` para garantir que o contexto sobe corretamente.

---

## Docker

### Build local da imagem
```bash
docker build -t projeto-tdd-ecommerce:local .
```

### Rodar o container
```bash
docker run --rm -p 8080:8080 projeto-tdd-ecommerce:local
```

Teste:
```bash
curl http://localhost:8080/health
```

---

## Kubernetes

### Namespace e recursos
Os manifests estão em `k8s/app/`:
- `namespace.yaml`
- `deployment.yaml`
- `service.yaml`
- `ingress.yaml`

Aplicar manualmente (no cluster):
```bash
kubectl apply -f k8s/app/namespace.yaml
kubectl apply -f k8s/app/
```

Verificar:
```bash
kubectl -n ecommerce get pods,svc,ingress
kubectl -n ecommerce logs deploy/ecommerce --tail=100
```

### Probes (liveness/readiness)
O Deployment utiliza `/health`:
- readiness: inicia após `15s`
- liveness: inicia após `30s`

---

## Ingress, DNS e HTTPS

### DNS gratuito (nip.io)
Para evitar dependência de domínio pago, utiliza-se `nip.io`, que resolve um hostname diretamente para o IP:

- Hostname: `app.72.60.13.231.nip.io`
- IP: `72.60.13.231`

Validação:
```bash
getent hosts app.72.60.13.231.nip.io
```

### Ingress Controller (NodePort)
O `ingress-nginx` foi instalado como NodePort:
- HTTP: `30080`
- HTTPS: `30443`


### Nginx (Edge Reverse Proxy)
O Nginx na VPS atende `80/443` e encaminha para o NodePort do Ingress.

Exemplo de configuração (`/etc/nginx/sites-available/ecommerce-k8s.conf`):

```nginx
server {
  listen 80;
  server_name app.72.60.13.231.nip.io;

  location / {
    proxy_pass http://127.0.0.1:30080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}

server {
  listen 443 ssl;
  server_name app.72.60.13.231.nip.io;

  ssl_certificate     /etc/letsencrypt/live/app.72.60.13.231.nip.io/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/app.72.60.13.231.nip.io/privkey.pem;

  location / {
    proxy_pass http://127.0.0.1:30080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
  }
}
```

Ativar e recarregar:
```bash
sudo ln -sf /etc/nginx/sites-available/ecommerce-k8s.conf /etc/nginx/sites-enabled/ecommerce-k8s.conf
sudo nginx -t
sudo systemctl reload nginx
```

### Certificado HTTPS (Let’s Encrypt)
Emissão do certificado:
```bash
sudo certbot certonly --nginx -d app.72.60.13.231.nip.io
```

Teste:
```bash
curl -v https://app.72.60.13.231.nip.io/health
```

---

## CI/CD (GitHub Actions)

Workflow principal: `.github/workflows/pipeline.yml`

### CI
- Checkout
- Setup Java 17 + cache Maven
- `mvn clean verify`
- Build e push da imagem para GHCR:
  - `ghcr.io/eduardoassisac/projeto-de-tdd-ecommerce:latest`
  - `ghcr.io/eduardoassisac/projeto-de-tdd-ecommerce:<GIT_SHA>`

### CD
- Configura `kubectl` via kubeconfig armazenado em segredo
- Aplica manifests e atualiza a imagem do Deployment com a tag do commit

#### Secret necessário
- `KUBE_CONFIG_DATA`: conteúdo base64 do kubeconfig do cluster (k3s)

Exemplo (na VPS):
```bash
sudo cat /etc/rancher/k3s/k3s.yaml | base64 -w 0
```
---




