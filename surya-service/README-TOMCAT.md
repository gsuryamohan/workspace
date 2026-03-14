# Install Tomcat on surya-service EC2

This installs **Apache Tomcat** on your **surya-service** EC2 instance, choosing a version that matches the Java already installed:

| Java version | Tomcat version installed |
|--------------|---------------------------|
| Java 17+     | Tomcat 11.0.x             |
| Java 11–16   | Tomcat 10.1.x             |
| Java 8–10    | Tomcat 9.0.x              |

## Prerequisites

- **Java** already installed on the instance (OpenJDK 8, 11, or 17).
- Instance has outbound HTTPS (port 443) to download from Apache.

## Option 1: Run via SSH (recommended)

1. **Copy the script to the instance**

   From your Mac (replace with your key and instance DNS or IP):

   ```bash
   scp -i /path/to/your-key.pem surya-service/install-tomcat.sh ec2-user@<surya-service-public-ip-or-dns>:/tmp/
   ```

   Use `ubuntu@...` if the AMI user is `ubuntu` instead of `ec2-user`.

2. **SSH in and run it**

   ```bash
   ssh -i /path/to/your-key.pem ec2-user@<surya-service-public-ip-or-dns>
   sudo bash /tmp/install-tomcat.sh
   ```

3. **Open port 8080 in the EC2 security group**

   In AWS Console: EC2 → Security Groups → group attached to surya-service → Inbound rules → Add rule: Type **Custom TCP**, Port **8080**, Source (e.g. **My IP** or **0.0.0.0/0** for testing).

4. **Check Tomcat**

   In a browser: `http://<surya-service-public-ip>:8080`  
   You should see the Tomcat default page.

## Option 2: Run via AWS Systems Manager (SSM)

If surya-service has the SSM agent and an IAM instance profile that allows SSM:

1. In **AWS Systems Manager** → **Run Command**, choose **AWS-RunShellScript**.
2. Targets: select the **surya-service** instance.
3. Paste the contents of `install-tomcat.sh` into the script box (or run a one-liner that downloads the script from a URL if you host it).
4. Run the command, then open port **8080** in the security group and visit `http://<instance-ip>:8080`.

## After install

- **Install path:** `/opt/tomcat`
- **Service:** `systemctl status tomcat` / `systemctl restart tomcat`
- **Logs:** `journalctl -u tomcat -f`
- **Deploy app:** put your WAR in `/opt/tomcat/webapps/` and (if needed) `systemctl restart tomcat`.

## Security note

For production, consider:

- Restricting security group access to 8080 (e.g. only from a load balancer or VPN).
- Enabling the Tomcat manager only from localhost or with strong credentials.
- Putting a reverse proxy (e.g. Nginx) in front and terminating TLS there.
