#!/bin/bash

# Colors
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           OpenAI API Key Setup Assistant                    ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if API key is already set
if [ ! -z "$OPENAI_API_KEY" ]; then
    echo -e "${GREEN}✓ OPENAI_API_KEY is already set!${NC}"
    echo -e "${CYAN}  Key prefix: ${OPENAI_API_KEY:0:7}...${OPENAI_API_KEY: -4}${NC}"
    echo ""
    echo "Would you like to:"
    echo "  1. Keep current key"
    echo "  2. Update to new key"
    read -p "Choice (1 or 2): " choice
    
    if [ "$choice" = "1" ]; then
        echo -e "${GREEN}Keeping current key. You're ready to go!${NC}"
        exit 0
    fi
fi

echo -e "${YELLOW}To use OpenAI, you need an API key.${NC}"
echo ""
echo "Get your API key from: ${CYAN}https://platform.openai.com/api-keys${NC}"
echo ""
echo -e "${YELLOW}⚠️  SECURITY: This script will add the key to your shell profile.${NC}"
echo -e "${YELLOW}    The key will NEVER be committed to git.${NC}"
echo ""

# Detect shell
SHELL_NAME=$(basename "$SHELL")
if [ "$SHELL_NAME" = "bash" ]; then
    PROFILE_FILE="$HOME/.bashrc"
    if [ -f "$HOME/.bash_profile" ]; then
        PROFILE_FILE="$HOME/.bash_profile"
    fi
elif [ "$SHELL_NAME" = "zsh" ]; then
    PROFILE_FILE="$HOME/.zshrc"
elif [ "$SHELL_NAME" = "fish" ]; then
    PROFILE_FILE="$HOME/.config/fish/config.fish"
else
    PROFILE_FILE="$HOME/.profile"
fi

echo -e "${CYAN}Detected shell: $SHELL_NAME${NC}"
echo -e "${CYAN}Profile file: $PROFILE_FILE${NC}"
echo ""

# Get API key
echo "Please paste your OpenAI API key (starts with sk-):"
read -s API_KEY
echo ""

# Validate format
if [[ ! $API_KEY =~ ^sk- ]]; then
    echo -e "${RED}✗ Invalid API key format. Keys should start with 'sk-'${NC}"
    exit 1
fi

# Add to profile
echo ""
echo "Adding to $PROFILE_FILE..."

# Check if already in profile
if grep -q "OPENAI_API_KEY" "$PROFILE_FILE" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  OPENAI_API_KEY already exists in $PROFILE_FILE${NC}"
    echo "Please edit manually to update: nano $PROFILE_FILE"
else
    echo "" >> "$PROFILE_FILE"
    echo "# OpenAI API Key (added by setup-openai.sh)" >> "$PROFILE_FILE"
    echo "export OPENAI_API_KEY=\"$API_KEY\"" >> "$PROFILE_FILE"
    echo -e "${GREEN}✓ Added to $PROFILE_FILE${NC}"
fi

# Set for current session
export OPENAI_API_KEY="$API_KEY"
echo -e "${GREEN}✓ Set for current session${NC}"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                    Setup Complete!                           ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Your API key is now configured."
echo ""
echo "To use it:"
echo -e "  1. ${CYAN}Current terminal:${NC} Already active, just run demos"
echo -e "  2. ${CYAN}New terminals:${NC} Run: source $PROFILE_FILE"
echo -e "  3. ${CYAN}Or:${NC} Close and reopen terminal"
echo ""
echo "Run the OpenAI demo:"
echo -e "  ${CYAN}mvn exec:java -Dexec.mainClass=\"com.ververica.flink.agent.example.OpenAIFlinkAgentsDemo\"${NC}"
echo ""
echo -e "${YELLOW}⚠️  Remember: Never commit API keys to git!${NC}"
