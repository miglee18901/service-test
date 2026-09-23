# Service Test

A simple Spring Boot service for managing executions and scheduling them with dynamic cron jobs.

## Features

- Manage cron jobs with validation, search, pagination, and sorting.
- Assign each execution to at most one cron job.
- Enable or disable individual scheduled executions.
- Enable or disable all execution mappings belonging to a cron job.
- Register, cancel, and reschedule tasks dynamically with `TaskScheduler`.
- Restore active cron jobs when the application starts.
- Execute the existing `start(executionInfoId, userDetails)` flow from a scheduled task.
- Use a mock execution API client, so the start flow can be tested without an external service.

## Technology

- Java 8
- Spring Boot 2.7.18
- Spring Web
- Spring Data JPA
- Bean Validation
- MySQL 8
- c3p0 connection pool
- Maven
- JUnit 5 and Mockito

## Database configuration

Create a `.env` file in the project root:

```properties
DB_URL=jdbc:mysql://localhost:3306/service-test?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Barnaul
DB_USERNAME=root
DB_PASSWORD=your_password
```

The `.env` file is ignored by Git. Do not commit database credentials.

The application uses `spring.jpa.hibernate.ddl-auto=update`, so Hibernate creates or updates the required tables when the application starts.

The scheduling feature uses these tables:

```sql
CREATE TABLE cronjob (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    cron_value VARCHAR(100) NOT NULL,
    CONSTRAINT uk_cronjob_name UNIQUE (name)
);

CREATE TABLE cronjob_execution (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    cronjob_id        BIGINT NOT NULL,
    execution_info_id BIGINT NOT NULL,
    status            TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_ce_cronjob FOREIGN KEY (cronjob_id) REFERENCES cronjob(id),
    CONSTRAINT fk_ce_execution FOREIGN KEY (execution_info_id) REFERENCES execution_info(id),
    CONSTRAINT uk_cronjob_execution UNIQUE (cronjob_id, execution_info_id),
    CONSTRAINT uk_execution_info_cronjob UNIQUE (execution_info_id)
);
```

The unique constraint on `execution_info_id` ensures that one execution belongs to only one cron job.

## Run the application

```bash
mvn spring-boot:run
```

The service starts at `http://localhost:8080`.

## Run tests

```bash
mvn test
```

## API conventions

- `GET` and `DELETE` endpoints receive the resource ID in the URL.
- Resource IDs are passed in URL path variables for `GET`, `PUT`, `PATCH`, and `DELETE` endpoints.
- Pagination uses Spring parameters such as `page`, `size`, and `sort`.
- Page size must be between 1 and 100.

## Cron job API

### Create a cron job

```http
POST /api/cronjobs
Content-Type: application/json

{
  "name": "Run every five minutes",
  "cronValue": "0 */5 * * * *"
}
```

### Get or delete a cron job

```http
GET /api/cronjobs/1
DELETE /api/cronjobs/1
```

### Search cron jobs

```http
GET /api/cronjobs?keyword=five&page=0&size=20&sort=name,asc
```

Supported sort properties: `id`, `name`, and `cronValue`.

### Update a cron job

```http
PUT /api/cronjobs/1
Content-Type: application/json

{
  "name": "Run every ten minutes",
  "cronValue": "0 */10 * * * *"
}
```

Changing `cronValue` cancels the old scheduled task and registers it again with the new expression.

### Change all mapping statuses

The client sends every mapping ID displayed on the page together with its expected current status. If another session has changed a status, the request returns a conflict and the client should reload the page.

```http
PATCH /api/cronjobs/1/executions/status
Content-Type: application/json

{
  "status": true,
  "items": [
    {
      "id": 10,
      "expectedStatus": false
    },
    {
      "id": 11,
      "expectedStatus": false
    }
  ]
}
```

## Cron job execution API

### Add an execution to a cron job

```http
POST /api/cronjob-executions
Content-Type: application/json

{
  "cronjobId": 1,
  "executionInfoId": 100,
  "status": true
}
```

### Get or delete a mapping

```http
GET /api/cronjob-executions/10
DELETE /api/cronjob-executions/10
```

### Search mappings

```http
GET /api/cronjob-executions?keyword=nightly&cronjobId=1&executionInfoId=100&status=true&page=0&size=20&sort=id,desc
```

Supported sort properties: `id`, `status`, `cronjob.id`, and `executionInfo.id`.

### Update a mapping

```http
PUT /api/cronjob-executions/10
Content-Type: application/json

{
  "cronjobId": 1,
  "executionInfoId": 100,
  "status": true
}
```

### Change one mapping status

```http
PATCH /api/cronjob-executions/10/status
Content-Type: application/json

{
  "expectedStatus": true,
  "status": false
}
```

## Execution API

```http
GET /api/execution-info?page=0&size=20
GET /api/executionInfo/execute-vim
POST /api/execution-info/100/start
X-User: operator
```

The start endpoint invokes the same service method used by scheduled executions.

The execute-vim endpoint returns `execute-vim.options` from application configuration. For example: `execute-vim.options=vim-a,vim-b`. If unset, it returns an empty list.

## Scheduler behavior

- A cron job is registered only when it has at least one enabled `cronjob_execution` mapping.
- At trigger time, the scheduler reloads enabled mappings from the database and starts each execution.
- If every mapping is disabled, the corresponding scheduled task is cancelled to avoid unnecessary memory usage.
- Enabling a mapping registers its cron job again when necessary.
- Active schedules are reconstructed from the database after an application restart.

An abrupt shutdown cannot roll back work already committed by an external service or a completed database transaction. In-progress local database transactions are normally rolled back when their connection closes, but an execution may remain partially processed if external side effects occurred before shutdown. Production deployments should make the start operation idempotent and persist execution state so interrupted work can be detected and retried safely.
