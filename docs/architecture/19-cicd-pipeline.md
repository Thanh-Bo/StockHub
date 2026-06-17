# Section 19: CI/CD Pipeline

## 19.1 Pipeline Overview (GitHub Actions)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        GITHUB ACTIONS PIPELINE                           │
│                                                                          │
│  PUSH to main / PR ──────► ┌─────────────────────────────┐               │
│                             │      CI PIPELINE             │               │
│                             │  (ci.yml)                   │               │
│                             │                             │               │
│                             │  1. Checkout                │               │
│                             │  2. Setup Java 21           │               │
│                             │  3. Setup Node 22           │               │
│                             │  4. Cache Maven deps        │               │
│                             │  5. Cache npm deps          │               │
│                             │  6. Compile (Maven)         │               │
│                             │  7. Unit Tests              │               │
│                             │  8. Repository Tests 🐳     │               │
│                             │  9. Integration Tests 🐳    │               │
│                             │  10. API Tests              │               │
│                             │  11. Lint (Checkstyle)      │               │
│                             │  12. Security Scan          │               │
│                             │      (Trivy / Snyk)         │               │
│                             │  13. Build Angular          │               │
│                             │  14. Build Docker Image     │               │
│                             └─────────────┬───────────────┘               │
│                                           │                               │
│                          ┌────────────────┴────────────────┐              │
│                          │  On main branch only:           │              │
│                          ▼                                 ▼              │
│              ┌─────────────────────┐           ┌─────────────────────┐     │
│              │  CD: Backend (Azure)│           │  CD: Frontend (Vercel)│   │
│              │  (cd-backend.yml)   │           │  (cd-frontend.yml)   │    │
│              │                     │           │                      │    │
│              │  1. Push to GHCR    │           │  1. Deploy to Vercel │    │
│              │  2. Deploy to Azure │           │     (Production)     │    │
│              │     App Service     │           │                      │    │
│              │  3. Run migrations  │           │                      │    │
│              │  4. Health check    │           │                      │    │
│              └─────────────────────┘           └─────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 19.2 CI Pipeline (ci.yml)

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend-tests:
    name: Backend Tests
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Compile
        run: mvn compile -B -q

      - name: Unit Tests
        run: mvn test -pl '!integration-tests' -B

      - name: Repository & Integration Tests
        run: mvn verify -pl integration-tests -B
        env:
          SPRING_DATASOURCE_URL: jdbc:tc:postgresql:16:///stockhub_test
          SPRING_REDIS_HOST: localhost

      - name: Checkstyle
        run: mvn checkstyle:check -B

      - name: Dependency Check (OWASP)
        run: mvn dependency-check:check -B || true  # Non-blocking for now

  frontend-tests:
    name: Frontend Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install Dependencies
        run: npm ci
        working-directory: frontend

      - name: Lint
        run: npm run lint
        working-directory: frontend

      - name: Build
        run: npm run build -- --configuration=production
        working-directory: frontend

      - name: Unit Tests (Karma/Jasmine)
        run: npm run test -- --watch=false --browsers=ChromeHeadless
        working-directory: frontend

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Trivy Docker Image Scan (pre-build check)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: 'backend/'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload SARIF
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'

  build-docker:
    name: Build Docker Image
    needs: [backend-tests, frontend-tests]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker Image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/stockhub-api:${{ github.sha }} \
            -f backend/Dockerfile backend/

      - name: Save Image (for CD)
        if: github.ref == 'refs/heads/main'
        run: |
          docker save ghcr.io/${{ github.repository }}/stockhub-api:${{ github.sha }} \
            | gzip > stockhub-api.tar.gz

      - name: Upload Artifact
        if: github.ref == 'refs/heads/main'
        uses: actions/upload-artifact@v4
        with:
          name: docker-image
          path: stockhub-api.tar.gz
```

---

## 19.3 CD Pipeline — Backend (cd-backend.yml)

```yaml
name: CD - Backend (Azure)

on:
  workflow_run:
    workflows: [CI]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest

    steps:
      - name: Download Docker Image
        uses: actions/download-artifact@v4
        with:
          name: docker-image

      - name: Load Docker Image
        run: docker load < stockhub-api.tar.gz

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Push to GHCR
        run: |
          docker tag ghcr.io/${{ github.repository }}/stockhub-api:${{ github.sha }} \
            ghcr.io/${{ github.repository }}/stockhub-api:latest
          docker push ghcr.io/${{ github.repository }}/stockhub-api:${{ github.sha }}
          docker push ghcr.io/${{ github.repository }}/stockhub-api:latest

      - name: Deploy to Azure App Service
        uses: azure/webapps-deploy@v3
        with:
          app-name: stockhub-api
          publish-profile: ${{ secrets.AZURE_WEBAPP_PUBLISH_PROFILE }}
          images: ghcr.io/${{ github.repository }}/stockhub-api:${{ github.sha }}

      - name: Run Flyway Migrations
        run: |
          # Migration runs automatically on Spring Boot startup
          # Health check verifies migration completed
          sleep 30
          curl -f https://stockhub-api.azurewebsites.net/actuator/health

      - name: Notify on Failure
        if: failure()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "❌ Backend deployment failed for ${{ github.sha }}"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 19.4 CD Pipeline — Frontend (cd-frontend.yml)

```yaml
name: CD - Frontend (Vercel)

on:
  workflow_run:
    workflows: [CI]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'

      - name: Install Dependencies
        run: npm ci
        working-directory: frontend

      - name: Build (Production)
        run: npm run build -- --configuration=production
        working-directory: frontend
        env:
          API_URL: https://stockhub-api.azurewebsites.net

      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          vercel-args: '--prod'
          working-directory: frontend
```

---

## 19.5 Dockerfile (Backend)

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S stockhub && adduser -S stockhub -G stockhub
USER stockhub

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseZGC", "-Xmx512m", "-jar", "app.jar"]
```

---

## 19.6 Environment Variables & Secrets

| Secret | Purpose | Stored In |
|--------|---------|-----------|
| `JWT_PRIVATE_KEY` | RS256 JWT signing key | GitHub Secrets → Azure App Settings |
| `JWT_PUBLIC_KEY` | RS256 JWT verification key | GitHub Secrets |
| `DB_URL` | PostgreSQL connection string | Azure App Settings |
| `DB_USERNAME` | Database username | Azure App Settings |
| `DB_PASSWORD` | Database password | Azure Key Vault → App Settings |
| `REDIS_URL` | Upstash Redis URL | Azure App Settings |
| `REDIS_PASSWORD` | Upstash Redis password | Azure Key Vault |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | Azure App Settings |
| `GOOGLE_CLIENT_SECRET` | Google OAuth secret | Azure Key Vault |
| `VERCEL_TOKEN` | Vercel deployment token | GitHub Secrets |
| `AZURE_WEBAPP_PUBLISH_PROFILE` | Azure deploy credential | GitHub Secrets |
