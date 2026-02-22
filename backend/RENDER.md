# Deploying NexaEDI backend on Render

## 1. Create a PostgreSQL database (Render Dashboard)

- Create a **PostgreSQL** instance.
- Note the **Internal Database URL** (or use the connection fields from the Connect tab).

## 2. Create a Web Service (Docker)

- **Root Directory:** `backend`
- **Dockerfile path:** `Dockerfile`
- **Instance type:** Free or paid as needed.

## 3. Required environment variables

Set these in the Web Service **Environment** tab:

| Variable | Required | Example / notes |
|----------|----------|------------------|
| `SPRING_PROFILES_ACTIVE` | **Yes** | `prod` |
| `SPRING_DATASOURCE_URL` | **Yes** | `jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/nexaedi` (use your **Internal** host from the Postgres Connect tab) |
| `SPRING_DATASOURCE_USERNAME` | **Yes** | Database username from Render Postgres |
| `SPRING_DATASOURCE_PASSWORD` | **Yes** | Database password from Render Postgres |
| `SPRING_DATASOURCE_URL_OPTIONS` | Recommended for Render Postgres | `?sslmode=require` |

**Converting Render Internal URL to JDBC**

Render shows a URL like:

`postgres://user:password@hostname:5432/dbname`

Use it as follows:

- **SPRING_DATASOURCE_URL:** `jdbc:postgresql://hostname:5432/dbname` (replace `hostname`, `5432`, `dbname` from the Connect tab; do **not** put the password in the URL).
- **SPRING_DATASOURCE_USERNAME:** `user` from the URL.
- **SPRING_DATASOURCE_PASSWORD:** `password` from the URL (or from the Render Postgres credentials).
- **SPRING_DATASOURCE_URL_OPTIONS:** `?sslmode=require` (Render Postgres uses SSL).

## 4. Optional (JWT, S3, etc.)

- `JWT_SECRET` — strong secret for auth tokens.
- `AWS_REGION`, `S3_BUCKET_NAME`, etc. — if you use S3 for EDI storage.

## 5. Linking Postgres to the Web Service

In the Render Dashboard, open your PostgreSQL instance → **Connect** → choose your Web Service. Render can inject a single `DATABASE_URL`; the app expects the separate `SPRING_DATASOURCE_*` variables above, so set them manually (or map `DATABASE_URL` into JDBC URL and username/password yourself).

After saving env vars, redeploy. The app will listen on `PORT` (set by Render, default 8080).
