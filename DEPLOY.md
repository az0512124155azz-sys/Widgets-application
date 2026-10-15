# Deploy (Vercel)
1. Vercel -> Add New Project -> import this GitHub repo (Framework: Other).
2. Project -> Storage -> Marketplace -> Upstash Redis (free) -> connect to the project.
3. Settings -> Environment Variables: MCP_KEY = a long random string. Redeploy.
4. Claude -> Settings -> Connectors -> Add custom connector:
   https://YOUR-PROJECT.vercel.app/api/mcp?key=YOUR_MCP_KEY
5. In the phone app: server URL = https://YOUR-PROJECT.vercel.app , key = YOUR_MCP_KEY, tap Sync.
