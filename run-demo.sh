#!/bin/bash

# Colors
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║          Simulated Agent Demo - VISUALIZATION ONLY            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}⚠️  WARNING: This is a SIMULATION using mock data${NC}"
echo -e "${YELLOW}   It demonstrates architecture concepts, not real agent execution${NC}"
echo -e "${YELLOW}   For real examples, see TieredAgentExample.java (coming in v1.0)${NC}"
echo ""

# Check if compiled
if [ ! -d "target/classes" ]; then
    echo -e "${YELLOW}Compiling project...${NC}"
    mvn compile -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}Compilation failed. Please check errors above.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Compilation successful${NC}"
    echo ""
fi

# Run the demo
echo -e "${GREEN}Starting simulated demo...${NC}"
echo ""
mvn exec:java -Dexec.mainClass="com.ververica.flink.agent.plugins.flintagents.examples.SimulatedAgentDemo" -P flink-agents -q

echo ""
echo -e "${GREEN}Demo session ended.${NC}"
