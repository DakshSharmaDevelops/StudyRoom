# Studyroom

Studyroom is a Spring Boot tutoring-center app for teacher-managed classes,
learning materials, quizzes, student messages, and AI assistance grounded in
the teacher's notes.

## Getting started

See [HELP.md](HELP.md) for PostgreSQL setup, teacher account creation, SMTP
configuration for password recovery, and AI configuration.

The local PostgreSQL service is defined in `compose.yaml`. Copy
`.env.example` to `.env`, set a private database password, start PostgreSQL
with `docker compose up -d postgres`, and run the app with `./mvnw
spring-boot:run`.

Local databases, uploaded files, secrets, and build output are excluded from
version control.
