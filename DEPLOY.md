# Deploy (Vercel)
Storage: Vercel Blob (store connected to the project, env BLOB_READ_WRITE_TOKEN).
Env: MCP_KEY = long random string.
Claude connector URL: https://YOUR-PROJECT.vercel.app/api/mcp?key=MCP_KEY
Phone app: server URL = https://YOUR-PROJECT.vercel.app , key = MCP_KEY
Health check: /api/widgets?key=MCP_KEY&selftest=1
