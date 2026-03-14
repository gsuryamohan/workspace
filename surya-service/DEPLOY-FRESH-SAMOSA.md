# Deploy Fresh Samosa to surya-service Tomcat

This deploys the **Fresh Samosa** React app to the Tomcat `webapps` folder on the **surya-service** EC2 instance.

## 1. Whitelist port 8080 (required)

In **AWS Console**:

1. Go to **EC2** → **Instances**
2. Select the **surya-service** instance
3. Open the **Security** tab → click the **Security group** link
4. Edit **Inbound rules** → **Add rule**:
   - **Type:** Custom TCP
   - **Port:** 8080
   - **Source:** `0.0.0.0/0` (any IP) or **My IP** (your IP only)
5. Save rules

## 2. Deploy

From your Mac (with SSH key at `~/.ssh/surya-service-key.pem`):

```bash
cd surya-service
chmod +x deploy-fresh-samosa.sh
./deploy-fresh-samosa.sh
```

Or with a custom key path:

```bash
./deploy-fresh-samosa.sh /path/to/your-key.pem
```

## 3. URL

After deployment and whitelisting port 8080:

**http://16.145.70.106:8080/fresh-samosa/**

(Tomcat root is at `http://16.145.70.106:8080/`)
