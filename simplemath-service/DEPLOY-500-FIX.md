# 500 error fix and deploy

## What was fixed

- **Cause:** `GET /` had no mapping, so the global exception handler returned **500** with `{"error":"INTERNAL_ERROR","message":"Unexpected error","violations":[]}`.
- **Change:** Added `RootController` with `GET /` returning a small JSON payload (service name and endpoints). Other API paths unchanged.
- **Build:** JAR rebuilt: `target/simplemath-service-0.0.1-SNAPSHOT.jar`.
- **Instance:** surya-service was **rebooted** (nothing is on 8080 until you deploy again).
- **Deploy script:** Updated to install a **systemd** unit so the app starts on boot and survives reboots.

## Apply the fix (one command)

From this repo, with your SSH key for surya-service:

```bash
cd /Users/suryamohang/Documents/workspace/simplemath-service
./deploy-to-surya-service.sh /path/to/surya-service-key.pem
```

If your key is at `~/.ssh/surya-service-key.pem`:

```bash
cd /Users/suryamohang/Documents/workspace/simplemath-service
./deploy-to-surya-service.sh
```

This will:

1. Copy the new JAR to the instance.
2. Install (if missing) and start `simplemath-service` systemd service.
3. Keep the service running across reboots.

Then check:

- Root: `curl -s http://16.145.70.106:8080/`
- Math: `curl -s -X POST http://16.145.70.106:8080/api/v1/math/add -H 'Content-Type: application/json' -d '{"a":2,"b":3}'`
