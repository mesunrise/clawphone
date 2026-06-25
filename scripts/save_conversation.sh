#!/bin/bash
# Save conversation summary (user messages + assistant final text) to project directory

PROJ_CONV_DIR="/personal/ai_workspace/clawp/docs/conversations"
SOURCE_DIR="$HOME/.claude/projects/-personal-ai-workspace-clawp"

mkdir -p "$PROJ_CONV_DIR"

# Find the most recently modified conversation JSONL
LATEST=$(find "$SOURCE_DIR" -maxdepth 1 -name "*.jsonl" -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2-)

if [ -z "$LATEST" ] || [ ! -f "$LATEST" ]; then
    exit 0
fi

SESSION_ID=$(basename "$LATEST" .jsonl)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DEST="$PROJ_CONV_DIR/${TIMESTAMP}_${SESSION_ID}.md"

python3 - "$LATEST" "$DEST" << 'PYEOF'
import json, sys, os

src, dst = sys.argv[1], sys.argv[2]
lines = []
with open(src, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            lines.append(json.loads(line))
        except:
            pass

output = []
output.append(f"# Conversation Summary\n")
output.append(f"Session: {os.path.basename(src)}\n")

for entry in lines:
    msg_type = entry.get("type", "")

    # User messages
    if msg_type == "user":
        msg = entry.get("message", {})
        content = msg.get("content", "")
        if isinstance(content, str) and content.strip():
            output.append(f"\n## User\n\n{content.strip()}\n")
        elif isinstance(content, list):
            text_parts = [p.get("text", "") for p in content if p.get("type") == "text"]
            combined = "\n".join(t for t in text_parts if t.strip())
            if combined:
                output.append(f"\n## User\n\n{combined}\n")

    # Assistant messages - only extract final text (no tool calls)
    elif msg_type == "assistant":
        msg = entry.get("message", {})
        content = msg.get("content", "")
        if isinstance(content, str) and content.strip():
            output.append(f"\n## Assistant\n\n{content.strip()}\n")
        elif isinstance(content, list):
            text_parts = [p.get("text", "") for p in content if p.get("type") == "text" and p.get("text", "").strip()]
            combined = "\n".join(text_parts)
            if combined.strip():
                output.append(f"\n## Assistant\n\n{combined.strip()}\n")

if len(output) > 2:
    with open(dst, 'w', encoding='utf-8') as f:
        f.write("\n".join(output))

PYEOF
