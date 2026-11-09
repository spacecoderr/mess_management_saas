# Deploying Thali for free (Neon + Render + Vercel + Brevo)

1. **GitHub**: create an empty repo, then `git remote add origin <url> && git push -u origin main`.
2. **Neon** (us-east-1; keep Render in Virginia): copy the *pooled* connection details. Build `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`, plus `DB_USER`, `DB_PASS`.
3. **Brevo**: restricted API key (Transactional email: All) and a verified sender -> `BREVO_API_KEY`, `MAIL_FROM`.
4. **Render**: New > Blueprint > pick the repo (reads `render.yaml`). Fill the `sync: false` variables. `CORS_ORIGINS` = your Vercel URL (set after step 5, then redeploy). Choose your own `BOOTSTRAP_ADMIN_*`.
5. **Vercel**: import the repo, Root Directory `frontend`, env `VITE_API_URL` = the Render URL (no trailing slash), `VITE_SHOW_DEMO=true`.
6. Smoke test: open the Vercel URL, wait for "API & database online", register with a college email and check the OTP mail (and spam), log in as admin, book, scan, pay.

Free-tier notes: Render sleeps when idle (first request 30-50 s). Never commit keys; put them only in the Render/Vercel dashboards.
