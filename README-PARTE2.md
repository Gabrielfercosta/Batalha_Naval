# Batalha Naval — Parte 2: Observabilidade, Resiliência e Kubernetes

## 📋 Visão Geral

Este documento descreve as implementações de:
- **Observabilidade**: Métricas (Prometheus), Traces distribuídos (OpenTelemetry + Tempo), Dashboards (Grafana)
- **Resiliência**: Cache (Redis), Rate Limiting (Bucket4j)
- **Deploy Kubernetes**: 2 réplicas, Ingress, Sticky Session

---

## 🚀 Deploy com Docker Compose (Desenvolvimento)

### Pré-requisitos
- Docker Desktop com WSL2 habilitado
- Docker Compose v2

### Subir toda a stack

```bash
cd C:\Users\gcosta\Downloads\Batalha_Naval
docker compose up --build -d
```

### Serviços disponíveis

| Serviço       | URL                        | Descrição                    |
|---------------|----------------------------|------------------------------|
| Frontend      | http://localhost:3000       | Aplicação React              |
| Backend API   | http://localhost:8080       | Spring Boot                  |
| Prometheus    | http://localhost:9090       | Métricas                     |
| Grafana       | http://localhost:3001       | Dashboards (admin/admin)     |
| Tempo         | http://localhost:3200       | Traces distribuídos          |

### Verificar se está funcionando

```bash
# Health check do backend
curl http://localhost:8080/actuator/health

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus

# Ver logs
docker compose logs -f backend
```

---

## 📊 Observabilidade

### O que foi implementado

1. **Métricas (Prometheus)**
   - Tempo de resposta de todas as requisições HTTP
   - Contador de requisições lentas (>500ms)
   - Métricas JVM (heap, threads, GC)
   - Métricas por endpoint (`@Timed`)

2. **Traces Distribuídos (OpenTelemetry → Tempo)**
   - Cada requisição gera um traceId propagado automaticamente
   - Visualização de spans no Grafana via datasource Tempo

3. **Logs Estruturados**
   - Formato JSON em produção (profile `docker`)
   - Inclui traceId e spanId para correlação com traces
   - Logs de requisições lentas com detalhes

4. **Dashboards Grafana (pré-configurados)**
   - Requisições por segundo
   - Latência p95
   - Requisições lentas
   - JVM Heap
   - Threads ativas

### Como consultar

1. Acesse Grafana em http://localhost:3001 (admin/admin)
2. Vá em Dashboards → "Batalha Naval - Observabilidade"
3. Para traces: Explorer → Selecione "Tempo" → busque por traceId

---

## 🛡️ Resiliência

### Cache (Redis + Spring Cache)

- **Configuração**: `CacheConfig.java` com Redis backend
- **TTLs**:
  - `salas-abertas`: 5 segundos (dados em tempo real)
  - `trivia-perguntas`: 30 minutos (dados estáveis)
- **Fallback**: Se Redis não estiver disponível, usa cache em memória (`simple`)

### Rate Limiting (Bucket4j)

- **Limite**: 100 requisições/minuto por IP
- **Exceções**: `/actuator/**` e `/ws/**` não são limitados
- **Resposta ao exceder**: HTTP 429 com JSON `{"erro": "Limite de requisições excedido..."}`
- **Headers**: `X-Rate-Limit-Remaining` em cada resposta

---

## ☸️ Deploy em Kubernetes (Minikube)

### Pré-requisitos

```bash
# Instalar Minikube (no WSL2)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Instalar kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install kubectl /usr/local/bin/kubectl

# Iniciar cluster
minikube start --driver=docker

# Habilitar Ingress Controller
minikube addons enable ingress
```

### Build das imagens Docker (dentro do Minikube)

```bash
# Usar o Docker do Minikube para build
eval $(minikube docker-env)

# Build do backend
docker build -t batalha-naval-backend:latest ./batalha_naval_back

# Build do frontend
docker build -t batalha-naval-frontend:latest ./batalha-naval_front
```

### Aplicar manifests

```bash
# Aplicar na ordem correta
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/config.yml
kubectl apply -f k8s/postgres.yml
kubectl apply -f k8s/redis.yml
kubectl apply -f k8s/observabilidade.yml
kubectl apply -f k8s/backend.yml
kubectl apply -f k8s/frontend.yml
kubectl apply -f k8s/ingress.yml
```

### Verificar deploy

```bash
# Ver todos os pods
kubectl get pods -n batalha-naval

# Ver serviços
kubectl get svc -n batalha-naval

# Ver ingress
kubectl get ingress -n batalha-naval

# Ver logs do backend
kubectl logs -f deployment/backend -n batalha-naval

# Verificar que há 2 réplicas do backend
kubectl get pods -n batalha-naval -l app=backend
```

### Acessar a aplicação

```bash
# Adicionar ao /etc/hosts (ou C:\Windows\System32\drivers\etc\hosts)
echo "$(minikube ip) batalha-naval.local" | sudo tee -a /etc/hosts

# Abrir no navegador
# http://batalha-naval.local
```

Ou via port-forward direto:

```bash
kubectl port-forward svc/frontend 3000:80 -n batalha-naval
kubectl port-forward svc/backend 8080:8080 -n batalha-naval
```

---

## 🏗️ Arquitetura no Kubernetes

```
                    ┌─────────────┐
                    │   Ingress   │
                    │  (Nginx)    │
                    │ Sticky Sess │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────┴──┐  ┌─────┴──┐  ┌─────┴──┐
        │Frontend│  │Backend │  │Backend │
        │ (Nginx)│  │Pod #1  │  │Pod #2  │
        └────────┘  └───┬────┘  └───┬────┘
                        │            │
              ┌─────────┴────────────┴─────────┐
              │                                │
        ┌─────┴──┐                       ┌─────┴──┐
        │Postgres│                       │ Redis  │
        └────────┘                       └────────┘
```

### Sticky Session

- **Nível Service**: `sessionAffinity: ClientIP` (timeout 1h)
- **Nível Ingress**: Cookie `BATALHA_STICKY` (1h de duração)
- **Motivo**: Partidas são armazenadas em memória no pod, então o jogador deve ficar no mesmo pod durante toda a partida

---

## 📁 Estrutura de Arquivos Criados

```
Batalha_Naval/
├── docker-compose.yml                    # Stack completa para desenvolvimento
├── k8s/                                  # Manifests Kubernetes
│   ├── namespace.yml
│   ├── config.yml                        # ConfigMap + Secret
│   ├── postgres.yml                      # PostgreSQL + PVC
│   ├── redis.yml                         # Cache Redis
│   ├── backend.yml                       # 2 réplicas + sticky session
│   ├── frontend.yml                      # Nginx + React
│   ├── ingress.yml                       # Ingress com cookie affinity
│   └── observabilidade.yml               # Tempo (tracing)
├── observabilidade/
│   ├── prometheus/prometheus.yml         # Scrape config
│   ├── tempo/tempo.yml                   # Config do Tempo
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/datasources.yml
│       │   └── dashboards/dashboards.yml
│       └── dashboards/batalha-naval.json # Dashboard pré-criado
└── batalha_naval_back/
    └── src/main/java/.../config/
        ├── ObservabilidadeConfig.java    # @Timed aspect
        ├── RequestTimingFilter.java      # Requisições lentas
        ├── CacheConfig.java             # Redis cache
        └── RateLimitFilter.java         # Rate limiting
```

---

## 🔧 Troubleshooting

```bash
# Pod não inicia? Ver eventos
kubectl describe pod <nome-do-pod> -n batalha-naval

# Backend não conecta no Postgres?
kubectl logs deployment/backend -n batalha-naval | grep "datasource"

# Verificar se Redis está ok
kubectl exec -it deployment/redis -n batalha-naval -- redis-cli ping

# Reiniciar um deployment
kubectl rollout restart deployment/backend -n batalha-naval
```
