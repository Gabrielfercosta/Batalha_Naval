#!/bin/sh
# Comprova que o Sticky Session mantem um cliente sempre no mesmo pod.
# Compara o contador de requisicoes de cada pod antes e depois de um lote
# de requisicoes disparadas de um unico cliente atraves do Service.

NS=batalha-naval
POD1=$(kubectl get pods -n $NS -l app=backend -o jsonpath='{.items[0].metadata.name}')
POD2=$(kubectl get pods -n $NS -l app=backend -o jsonpath='{.items[1].metadata.name}')

echo "POD1 = $POD1"
echo "POD2 = $POD2"
echo ""

contar() {
  kubectl exec -n $NS "$1" -- sh -c \
    'curl -s "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/actuator/health"' \
    2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2
}

A1=$(contar "$POD1")
A2=$(contar "$POD2")
echo "ANTES  -> POD1: ${A1:-0} | POD2: ${A2:-0}"

kubectl run stickyclient --rm -i --restart=Never --image=curlimages/curl:latest -n $NS -- \
  sh -c 'i=0; while [ $i -lt 20 ]; do curl -s -o /dev/null http://backend:8080/actuator/health; i=$((i+1)); done' \
  >/dev/null 2>&1

sleep 2
D1=$(contar "$POD1")
D2=$(contar "$POD2")
echo "DEPOIS -> POD1: ${D1:-0} | POD2: ${D2:-0}"
echo ""

DIFF1=$(awk "BEGIN {print ${D1:-0} - ${A1:-0}}")
DIFF2=$(awk "BEGIN {print ${D2:-0} - ${A2:-0}}")
echo "Requisicoes recebidas: POD1=+$DIFF1 | POD2=+$DIFF2"
echo ""
echo "Sticky Session OK se TODAS as 20 requisicoes foram para um unico pod."
