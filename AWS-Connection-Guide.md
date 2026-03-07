# Step-by-Step Guide: Connect to Your AWS Account

This guide walks you through connecting to AWS from your machine (terminal / Cursor) using the **AWS CLI** and credentials.

---

## Prerequisites

- An AWS account ([create one](https://aws.amazon.com/free/) if needed)
- Terminal access (e.g. Terminal, iTerm, or Cursor’s integrated terminal)

---

## Step 1: Install the AWS CLI

**On macOS (using Homebrew):**

```bash
brew install awscli
```

**Or using the official installer:**

```bash
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
```

**Verify installation:**

```bash
aws --version
```

You should see something like `aws-cli/2.x.x`.

---

## Step 2: Get Your AWS Credentials

You need an **Access Key ID** and **Secret Access Key** for an IAM user.

### Option A: Create credentials for your root or IAM user

1. Sign in to the [AWS Console](https://console.aws.amazon.com/).
2. Open **IAM** (search for “IAM” in the top search bar).
3. In the left menu, click **Users** → select your user (or create one).
4. Open the **Security credentials** tab.
5. Under **Access keys**, click **Create access key**.
6. Choose use case (e.g. **Command Line Interface (CLI)**), confirm, then **Next** → **Create access key**.
7. **Copy and save**:
   - **Access key ID**
   - **Secret access key**  
   (You won’t see the secret again.)

### Option B: Your organization uses AWS SSO

If you use **AWS IAM Identity Center (SSO)**:

1. Ask your admin for the **SSO start URL** and **SSO region**.
2. Configure and login with:
   ```bash
   aws configure sso
   ```
3. Follow the prompts (SSO URL, region, account, role).  
4. Use `aws sso login` when you need to sign in again.

---

## Step 3: Configure the AWS CLI

Run:

```bash
aws configure
```

You’ll be prompted for:

| Prompt              | What to enter |
|---------------------|----------------|
| **AWS Access Key ID**  | Your access key ID from Step 2 |
| **AWS Secret Access Key** | Your secret access key from Step 2 |
| **Default region name**  | e.g. `us-east-1`, `ap-south-1` |
| **Default output format** | `json` (recommended) or `table`, `yaml` |

Credentials are stored in:

- **macOS/Linux:** `~/.aws/credentials`
- **Config (region, etc.):** `~/.aws/config`

---

## Step 4: Verify the Connection

Test with a simple command:

```bash
aws sts get-caller-identity
```

You should see your **Account**, **UserId**, and **Arn**. If this works, you’re connected.

**Other checks:**

```bash
# List S3 buckets (if you have any)
aws s3 ls

# Describe EC2 instances in default region (if you have any)
aws ec2 describe-instances
```

---

## Step 5: (Optional) Use Named Profiles

To use multiple AWS accounts or roles:

1. Edit or create `~/.aws/credentials`:

   ```ini
   [default]
   aws_access_key_id = AKIA...
   aws_secret_access_key = ...

   [work]
   aws_access_key_id = AKIA...
   aws_secret_access_key = ...
   ```

2. Use a profile with `--profile`:

   ```bash
   aws s3 ls --profile work
   ```

3. Or set for the current shell:

   ```bash
   export AWS_PROFILE=work
   aws s3 ls
   ```

---

## Quick Reference

| Task              | Command / action |
|-------------------|------------------|
| Configure CLI     | `aws configure` |
| Configure SSO     | `aws configure sso` |
| SSO login         | `aws sso login` |
| Check identity    | `aws sts get-caller-identity` |
| Use a profile     | `aws <command> --profile <name>` |
| Set default profile | `export AWS_PROFILE=<name>` |

---

## Security Tips

- **Don’t commit** `~/.aws/credentials` or any file containing keys to Git.
- Prefer **IAM users with minimal permissions** over the root account.
- **Rotate access keys** periodically (IAM → User → Security credentials → Create access key, then delete the old one).
- Use **SSO** when your organization supports it.

---

## Troubleshooting

| Error | What to do |
|-------|------------|
| `Unable to locate credentials` | Run `aws configure` and enter keys, or set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`. |
| `An error occurred (InvalidClientTokenId)` | Access key is wrong or deleted; create a new key in IAM and run `aws configure` again. |
| `Access Denied` / `UnauthorizedOperation` | The IAM user needs more permissions; add policies in IAM for the services you use. |
| SSO session expired | Run `aws sso login`. |

---

You’re set. Use `aws <service> <command>` for any AWS service (e.g. `aws s3`, `aws ec2`, `aws lambda`).
