#!/bin/bash

# PCS API Dependency Checker
# This script validates that all required containers are running before starting bootWithCCD

set -e

echo "🔍 PCS API Dependency Checker"
echo "================================"

# Required containers for PCS API
REQUIRED_CONTAINERS=(
    "cftlib-rse-idam-simulator-1"
    "cftlib-shared-database-pg-1" 
    "cftlib-ccd-elasticsearch-1"
    "cftlib-xui-manage-org-1"
    "cftlib-xui-manage-cases-1"
)

# Required ports
REQUIRED_PORTS=(5062 6432 9200 3001 3000)

# Check containers are running
echo "📦 Checking required containers..."
MISSING_CONTAINERS=()

for container in "${REQUIRED_CONTAINERS[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        status=$(docker ps --format '{{.Names}}\t{{.Status}}' | grep "^${container}" | cut -f2)
        echo "✅ $container: $status"
    else
        echo "❌ $container: NOT RUNNING"
        MISSING_CONTAINERS+=("$container")
    fi
done

# Check port connectivity
echo ""
echo "🔌 Checking port connectivity..."
UNAVAILABLE_PORTS=()

for port in "${REQUIRED_PORTS[@]}"; do
    if command -v nc >/dev/null 2>&1 && nc -z localhost $port 2>/dev/null; then
        echo "✅ Port $port: Available"
    elif command -v telnet >/dev/null 2>&1 && timeout 2 telnet localhost $port </dev/null >/dev/null 2>&1; then
        echo "✅ Port $port: Available" 
    else
        echo "❌ Port $port: Unavailable"
        UNAVAILABLE_PORTS+=("$port")
    fi
done

# Summary and recommendations
echo ""
echo "📊 SUMMARY"
echo "=========="

if [ ${#MISSING_CONTAINERS[@]} -eq 0 ] && [ ${#UNAVAILABLE_PORTS[@]} -eq 0 ]; then
    echo "✅ All dependencies satisfied!"
    echo "🚀 Ready to run: ./gradlew bootWithCCD"
    exit 0
else
    echo "❌ Dependencies not satisfied:"
    
    if [ ${#MISSING_CONTAINERS[@]} -gt 0 ]; then
        echo "   Missing containers: ${MISSING_CONTAINERS[*]}"
    fi
    
    if [ ${#UNAVAILABLE_PORTS[@]} -gt 0 ]; then
        echo "   Unavailable ports: ${UNAVAILABLE_PORTS[*]}"
    fi
    
    echo ""
    echo "🛠️  RECOMMENDED ACTIONS:"
    echo "   1. Ensure Docker is running"
    echo "   2. Start CCD environment using the hmcts/rse-cft-lib plugin" 
    echo "   3. Wait for all containers to be healthy"
    echo "   4. Re-run this script to verify"
    
    exit 1
fi