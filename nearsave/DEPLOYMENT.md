# NearSave — Render Deployment Guide

This guide explains how to deploy the NearSave Spring Boot application and a free MySQL database on **Render** step-by-step.

---

## Step 1: Get a Free Hosted MySQL Database
Since Render only provides managed PostgreSQL, the easiest way to get a free MySQL database is using a free cloud provider like **Clever Cloud**.

1. Go to [Clever Cloud](https://www.clever-cloud.com/) and create a free account.
2. Click **Create...** -> **Add an add-on** -> Select **MySQL**.
3. Choose the **Free Shared Plan (Dev)** and click next.
4. Go to your database dashboard and copy your connection details:
   * **Host** (e.g. `brxx...-mysql.services.clever-cloud.com`)
   * **Database Name** (e.g. `brxx...`)
   * **User** (e.g. `uxxx...`)
   * **Password** (e.g. `pxxx...`)

---

## Step 2: Push your Code to GitHub
Ensure all your project files are pushed to a repository on GitHub.
Your repository structure should look like this:
```text
nearsave-project/
├── docker-compose.yml
├── nearsave/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/
│   └── ...
└── ...
```

---

## Step 3: Deploy on Render

1. Go to [Render](https://render.com/) and log in.
2. Click **New +** -> Select **Web Service**.
3. Connect your GitHub account and select your `nearsave-project` repository.
4. Configure the Web Service settings:
   * **Name:** `nearsave`
   * **Environment:** `Docker`
   * **Region:** Choose the one closest to you (e.g., Singapore/Oregon).
   * **Branch:** `main` (or whichever branch your code is on).
   * **Root Directory:** `nearsave`
   * **Dockerfile Path:** `Dockerfile`
5. Scroll down and click on **Advanced**.
6. Click **Add Environment Variable** and add the following keys using the details from **Step 1 (Clever Cloud)**:
   * `DB_HOST` = (Your Clever Cloud Database Host)
   * `DB_PORT` = `3306`
   * `DB_NAME` = (Your Clever Cloud Database Name)
   * `DB_USERNAME` = (Your Clever Cloud Database User)
   * `DB_PASSWORD` = (Your Clever Cloud Database Password)
7. Click **Create Web Service**.

Render will now pull your repository, build the Docker image using the Dockerfile inside `nearsave/`, and start the application. Once deployed, Render will provide you with a public URL (e.g., `https://nearsave.onrender.com`).
