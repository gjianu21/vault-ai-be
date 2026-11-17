# vault-ai-be

## Run locally

Requirements: Java 21, access to the Oracle database, and `vault-ai-auth-server` already running on http://localhost:9000.

1. Create a `.env` file in the project root (it is not committed):

   ```
   DB_URL=jdbc:oracle:thin:@<host>:<port>/<service>
   VAULT_APP_DB_PASSWORD=<password>
   OAUTH2_CLIENT_SECRET=<same value as in vault-ai-auth-server>
   INTERNAL_API_KEY=<same value as in vault-ai-auth-server>
   ```

2. Start the application with the `local` and `mock` profiles, from the project root:

   ```
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,mock
   ```

   On Windows PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,mock"`

3. Check that it is up: http://localhost:8080/actuator/health

Notes:

- The application reads the OpenID configuration from the auth server at startup. If the auth server is down, it does not start.
- Without the `mock` profile, startup fails with `app.agent.base-url is not set`.
- Swagger UI is available on the `local` profile: http://localhost:8080/swagger-ui.html
- To use the application, start `vault-ai-fe` (`npm run dev`) and open http://localhost:5173.