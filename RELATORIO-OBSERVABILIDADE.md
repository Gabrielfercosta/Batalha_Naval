# Relatório de Observabilidade e Performance — Batalha Naval

## Resumo

Este documento inclui métricas coletadas de análise de performance dos endpoints, testes de carga e problemas identificados.

---

## 1. Métricas Coletadas

A aplicação foi instrumentada com Prometheus + Grafana para coletar métricas em tempo real:

| Métrica | O que mede |
|---------|-----------|
| Requisições por segundo (RPS) | Volume de tráfego em cada endpoint |
| Latência HTTP | Tempo de resposta de cada requisição |
| Requisições lentas | Contagem de requisições acima de 500ms |
| Queries SQL lentas | Contagem de consultas ao banco acima de 100ms |
| Cache hit/miss | Eficiência do cache Redis |
| JVM Heap | Uso de memória da aplicação |
| Threads ativas | Quantidade de threads em execução |
| CPU | Uso de CPU do processo |
| Conexões de banco (HikariCP) | Pool de conexões ativas/pendentes |

---

## 2. Tempo de Resposta dos Endpoints

### Medição realizada com a aplicação rodando em Docker

| Endpoint | Tempo médio | Observação |
|----------|------------|------------|
| `GET /api/game/open` | 3-12ms | Listagem de salas, operação em memória |
| `GET /api/quiz/open` | 5-7ms | Listagem de salas quiz |
| `GET /api/minado/open` | 3-6ms | Listagem de salas minado |
| `POST /api/game/create` | 9ms | Criação de partida |
| `POST /api/auth/register` | 70-300ms | Mais lento — por causa do BCrypt |
| `POST /api/auth/login` | ~70ms | BCrypt na comparação de senha |
| `GET /actuator/health` | 2-6ms | Health check interno |
| `GET /actuator/prometheus` | 5-8ms | Coleta de métricas |

### Conclusão

- A grande maioria dos endpoints responde em **menos de 15ms**
- O único endpoint com tempo significativo é o de **registro/login** (~70ms), que é intencionalmente lento por causa da criptografia BCrypt (proteção contra brute-force)
- Nenhum endpoint apresentou tempo acima de 500ms em uso normal

---

## 3. Teste de Carga (k6)

### Ferramenta

Grafana k6 — ferramenta profissional de teste de carga que simula múltiplos usuários virtuais simultâneos e mede latência, throughput e taxa de erros.

### Cenário do teste

| Fase | Duração | Usuários virtuais |
|------|---------|-------------------|
| Ramp-up | 10s | 0 → 10 |
| Sustentação | 30s | 10 |
| Escalada | 10s | 10 → 30 |
| Sustentação alta | 30s | 30 |
| Ramp-down | 10s | 30 → 0 |

Cada usuário virtual executa o seguinte fluxo a cada iteração:
1. Registra ou faz login
2. Lista salas abertas (clássico, minado, quiz)
3. Cria uma partida e sai dela
4. Verifica health check

### Resultados

```
╔══════════════════════════════════════════════════════╗
║     RESULTADO DO TESTE DE CARGA - BATALHA NAVAL     ║
╚══════════════════════════════════════════════════════╝

Total de requisições: 2.280
Duração total: 91s
Usuários virtuais (máx): 30

── Tempo de resposta (todas as requisições) ──
  Média:  7ms
  p90:    5ms
  p95:    71ms
  Máx:    164ms

── Tempo por operação ──
  Login/Registro:  média 9ms  | p95 85ms
  Listar salas:    média 2ms  | p95 6ms
  Criar partida:   média 2ms  | p95 6ms

── Taxa de erros (excluindo rate limiting) ──
  11.94%

── Thresholds ──
  ✅ PASS  erros: rate < 15%
  ✅ PASS  tempo_listar_salas: p95 < 100ms
  ✅ PASS  http_req_duration: p95 < 500ms
```

### Análise dos resultados

| Métrica | Valor | Avaliação |
|---------|-------|-----------|
| Tempo médio de resposta | 7ms | Excelente — abaixo de 50ms |
| p95 (95% das requisições) | 71ms | Bom — o BCrypt de login puxa pra cima |
| Tempo máximo | 164ms | Nenhuma requisição travou |
| Listar salas (média) | 2ms | Cache Redis servindo instantaneamente |
| Criar partida (média) | 2ms | Operação em memória, rápida |
| Login/Registro (p95) | 85ms | Esperado pelo BCrypt |
| Taxa de erros reais | 11.94% | Maioria por conflito de username repetido |

### Comportamento do Rate Limiting sob carga

Com 30 usuários simultâneos, o rate limiting (100 req/min por IP) bloqueou corretamente as requisições excedentes com HTTP 429. Isso é **comportamento esperado e desejado** — prova que o sistema se protege automaticamente contra sobrecarga.

Os 429 não são contabilizados como erros no teste porque são uma resposta intencional de proteção.

### Conclusão

A aplicação manteve **tempo de resposta médio de 7ms** mesmo com 30 usuários simultâneos gerando 2.280 requisições em 90 segundos. Nenhuma requisição ultrapassou 164ms. O rate limiting protegeu adequadamente contra abuso, e o cache manteve as consultas de listagem em 2ms.

### Como reproduzir

```powershell
cd C:\Users\gcosta\Downloads\Batalha_Naval
docker compose up -d
docker run --rm --network=batalha_naval_default -v ${PWD}:/scripts grafana/k6:latest run -e "BASE_URL=http://backend:8080" /scripts/k6-teste-carga.js
```

---

## 4. Rate Limiting sob Carga

### Metodologia anterior (teste simples)

Foram disparadas **150 requisições consecutivas** a partir de um único IP contra o endpoint `/actuator/health`.

### Resultado

| Código HTTP | Quantidade | Significado |
|-------------|-----------|-------------|
| 200 (OK) | ~85-104 | Requisições aceitas normalmente |
| 429 (Too Many Requests) | ~46-65 | Bloqueadas pelo rate limiting |

### Análise

- O rate limiting (100 req/minuto por IP) funcionou corretamente
- Após esgotar os 100 tokens, requisições adicionais são rejeitadas com mensagem clara
- O header `X-Rate-Limit-Remaining` informa quantas requisições restam
- Isso protege a aplicação contra ataques de negação de serviço (DDoS) e abuso de API

---

## 5. Cache — Análise de Eficiência

### O que foi cacheado

O endpoint `/api/game/open` (e equivalentes de quiz/minado) é consultado a cada 5 segundos por cada jogador que está no lobby. Sem cache, cada consulta percorre todas as partidas em memória.

### Teste realizado

15 requisições consecutivas ao mesmo endpoint:

| Métrica | Valor |
|---------|-------|
| Cache hits | 14 |
| Cache misses | 1 |
| Taxa de acerto | **93.3%** |

### Impacto

- Sem cache: 50 jogadores no lobby = 600 processamentos/minuto
- Com cache (TTL 5s): mesmos 50 jogadores = 12 processamentos/minuto
- **Redução de 98% na carga de processamento** nesse endpoint

---

## 6. Traces Distribuídos — O que encontramos

### Como funciona

Cada requisição recebe um `traceId` único. Dentro desse trace, cada operação (consulta ao banco, verificação de segurança, etc.) vira um "span" com duração própria.

### Spans detectados nos traces

| Span | O que representa |
|------|-----------------|
| `http post /api/auth/register` | Requisição HTTP completa |
| `secured request` | Processamento do filtro de segurança |
| `connection` | Abertura de conexão com o banco |
| `query` | Execução de SQL (com o SQL completo como atributo) |
| `result-set` | Leitura dos resultados da query |

### Exemplo real de um trace de registro

```
http post /api/auth/register (total: 74ms)
├── secured request (1ms)
├── connection (0.5ms)
├── query: "select u1_0.id from usuarios where username=?" (2ms)
├── connection (0.3ms)
└── query: "insert into usuarios (senha,username) values (?,?)" (3ms)
```

**Conclusão:** O tempo de 74ms é dominado pelo BCrypt (~65ms internos), não pelas queries de banco (que somam ~5ms). As consultas ao banco estão performando bem.

---

## 7. Problemas Identificados e Ações Tomadas

### Problema 1: Sem proteção contra abuso de requisições

**Antes:** Qualquer usuário ou bot poderia enviar milhares de requisições sem limitação, potencialmente derrubando o servidor.

**Solução:** Rate limiting com Bucket4j — 100 requisições/minuto por IP. Requisições excedentes recebem HTTP 429.

---

### Problema 2: Endpoint de listagem de salas consultado excessivamente

**Antes:** O lobby do frontend consulta as salas abertas a cada 5 segundos. Com N jogadores online, são N requisições a cada 5s — todas recalculando a mesma lista.

**Solução:** Cache Redis com TTL de 5 segundos. Apenas a primeira requisição de cada ciclo processa de fato; as demais são servidas instantaneamente do cache.

**Resultado medido:** taxa de acerto de 66-93% dependendo do padrão de tráfego.

---

### Problema 3: Sem visibilidade sobre o que acontece em produção

**Antes:** Se um usuário reclamasse de lentidão, não havia como saber o que estava acontecendo — sem métricas, sem logs estruturados, sem traces.

**Solução:**
- Prometheus coletando métricas a cada 15 segundos
- Grafana com dashboard mostrando RPS, latência, memória, CPU e cache
- Traces com OpenTelemetry mostrando o caminho completo de cada requisição
- Filtro automático que detecta e loga requisições acima de 500ms e queries acima de 100ms

---

### Problema 4: Sem redundância — se o servidor cai, todos os jogadores são afetados

**Antes:** Uma única instância do backend. Se crashar, toda a aplicação fica fora do ar.

**Solução:** Deploy em Kubernetes com 2 réplicas do backend + health checks automáticos. Se um pod morre, o outro continua atendendo e o Kubernetes reinicia o que caiu.

Sticky session garante que jogadores permanecem no mesmo pod durante toda a partida (comprovado: 20/20 requisições do mesmo cliente foram para o mesmo pod).

---

## 8. Ferramentas Utilizadas

| Ferramenta | Função |
|-----------|--------|
| Prometheus | Coleta e armazena métricas |
| Grafana | Visualização em dashboards |
| Grafana Tempo | Armazena e consulta traces distribuídos |
| Redis | Cache de respostas frequentes |
| Bucket4j | Rate limiting por IP |
| OpenTelemetry | Instrumentação de traces |
| datasource-micrometer | Instrumentação de queries SQL |
| Kind (Kubernetes) | Orquestração com 2 réplicas |
| Nginx Ingress | Roteamento + sticky session via cookie |
| Grafana k6 | Teste de carga com usuários virtuais simultâneos |

---

## 9. Como reproduzir os testes

### Subir a aplicação
```bash
cd C:\Users\gcosta\Downloads\Batalha_Naval
docker compose up --build -d
```

### Executar verificação automatizada
```powershell
powershell -ExecutionPolicy Bypass -File verificar.ps1
```

### Ver dashboard
- Grafana: http://localhost:3001 (admin/admin)
- Prometheus: http://localhost:9090

### Verificar Kubernetes (WSL)
```bash
wsl -d Ubuntu
kind create cluster --name batalha-naval --config /mnt/c/Users/gcosta/Downloads/Batalha_Naval/k8s/kind-config.yml
# ... aplicar manifests (ver README-PARTE2.md)
sh /mnt/c/Users/gcosta/Downloads/Batalha_Naval/k8s/verificar-sticky.sh
```
